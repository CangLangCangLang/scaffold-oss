package com.scaffold.framework.aspectj;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.common.utils.ServletUtils;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.common.utils.ip.IpUtils;
import com.scaffold.framework.manager.AsyncManager;
import com.scaffold.framework.manager.factory.AsyncFactory;
import com.scaffold.system.domain.SysAuditLog;
import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link AuditLog} 注解切面：执行前抓 before、执行后抓 after，
 * 计算 RFC 6902 JSON Patch，异步落 sys_audit_log。
 * <p>
 * 全局通用敏感字段（password / oldPassword / newPassword / confirmPassword）默认抹掉；
 * 业务可在 {@link AuditLog#excludeFields()} 中追加。
 *
 * @author scaffold
 */
@Aspect
@Component
public class AuditLogAspect
{
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    /** 与 LogAspect 对齐的全局敏感字段名 */
    public static final String[] DEFAULT_EXCLUDE_FIELDS = {
            "password", "oldPassword", "newPassword", "confirmPassword", "salt"
    };

    /** 单条快照的最大长度（防止超大对象写爆 LONGTEXT） */
    private static final int SNAPSHOT_MAX_LENGTH = 16 * 1024;

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, AuditLog audit) throws Throwable
    {
        long start = System.currentTimeMillis();
        // 1. 在方法执行前，按 SpEL 解析 before 快照（用 args，不带 result）
        Object beforeObject = evalBefore(pjp, audit);
        Throwable error = null;
        Object result = null;
        try
        {
            result = pjp.proceed();
            return result;
        }
        catch (Throwable ex)
        {
            error = ex;
            throw ex;
        }
        finally
        {
            // 不阻断业务，所有异常都吞
            try
            {
                SysAuditLog record = build(pjp, audit, beforeObject, result, error,
                        System.currentTimeMillis() - start);
                if (record != null)
                {
                    AsyncManager.me().execute(AsyncFactory.recordAudit(record));
                }
            }
            catch (Exception ignore)
            {
                log.warn("@AuditLog 处理异常 module={} action={} reason={}",
                        audit.module(), audit.action(), ignore.getMessage());
            }
        }
    }

    private Object evalBefore(ProceedingJoinPoint pjp, AuditLog audit)
    {
        if (StringUtils.isEmpty(audit.beforeProvider())) return null;
        try
        {
            return evalSpel(pjp, audit.beforeProvider(), null);
        }
        catch (Exception e)
        {
            log.warn("@AuditLog beforeProvider 解析失败 module={} expr={} reason={}",
                    audit.module(), audit.beforeProvider(), e.getMessage());
            return null;
        }
    }

    /** 在切面 around 内统一构造审计记录；返回 null 表示不入库（一般不发生）。 */
    private SysAuditLog build(ProceedingJoinPoint pjp, AuditLog audit,
                              Object beforeObject, Object result, Throwable error, long costMs)
    {
        SysAuditLog rec = new SysAuditLog();
        rec.setModule(audit.module());
        rec.setAction(audit.action());
        rec.setResourceType(StringUtils.isEmpty(audit.resourceType()) ? null : audit.resourceType());
        rec.setResourceId(safeEvalToString(pjp, audit.resourceId(), result));
        rec.setComment(safeEvalToString(pjp, audit.comment(), result));
        rec.setCostMs(costMs);
        rec.setStatus(error == null ? 0 : 1);
        if (error != null)
        {
            rec.setErrorMessage(StringUtils.substring(
                    error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(),
                    0, 2000));
        }

        // 操作人 / 部门
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null)
            {
                rec.setActor(loginUser.getUsername());
                rec.setActorId(loginUser.getUserId());
                SysUser u = loginUser.getUser();
                if (u != null)
                {
                    rec.setActorDeptId(u.getDeptId());
                    if (u.getDept() != null) rec.setActorDept(u.getDept().getDeptName());
                }
            }
        }
        catch (Exception ignore) { /* 未登录或 SecurityContext 不可用时跳过 */ }

        // request 上下文（trace / ip / uri）
        try
        {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null)
            {
                rec.setRequestUri(StringUtils.substring(request.getRequestURI(), 0, 255));
            }
            rec.setClientIp(IpUtils.getIpAddr());
        }
        catch (Exception ignore) { /* 非 HTTP 上下文（定时任务调用时）跳过 */ }
        rec.setTraceId(MDC.get("traceId"));

        // before / after 序列化 + diff
        String[] excludes = mergeExcludes(audit.excludeFields());
        String beforeJson = AuditDiffSupport.serialize(beforeObject, excludes);
        String afterJson = audit.recordReturn() ? AuditDiffSupport.serialize(result, excludes) : null;
        rec.setBeforeValue(AuditDiffSupport.truncate(beforeJson, SNAPSHOT_MAX_LENGTH));
        rec.setAfterValue(AuditDiffSupport.truncate(afterJson, SNAPSHOT_MAX_LENGTH));
        rec.setDiff(AuditDiffSupport.truncate(
                AuditDiffSupport.computeDiff(objectMapper, beforeJson, afterJson),
                SNAPSHOT_MAX_LENGTH));
        return rec;
    }


    private String[] mergeExcludes(String[] extra)
    {
        if (extra == null || extra.length == 0) return DEFAULT_EXCLUDE_FIELDS;
        String[] merged = Arrays.copyOf(DEFAULT_EXCLUDE_FIELDS, DEFAULT_EXCLUDE_FIELDS.length + extra.length);
        System.arraycopy(extra, 0, merged, DEFAULT_EXCLUDE_FIELDS.length, extra.length);
        return merged;
    }

    private String safeEvalToString(ProceedingJoinPoint pjp, String expression, Object result)
    {
        if (StringUtils.isEmpty(expression)) return null;
        try
        {
            Object v = evalSpel(pjp, expression, result);
            return v == null ? null : StringUtils.substring(v.toString(), 0, 255);
        }
        catch (Exception e)
        {
            log.debug("@AuditLog SpEL 解析失败 expr={} reason={}", expression, e.getMessage());
            return null;
        }
    }

    private Object evalSpel(ProceedingJoinPoint pjp, String expression, Object result)
    {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                pjp.getTarget(), method, pjp.getArgs(), PARAM_DISCOVERER);
        // 注册 bean resolver，让 @AuditLog 的 SpEL 表达式可以使用 @beanName.method() 形式
        // （典型用法：beforeProvider = "@sysUserService.selectUserById(#user.userId)"）
        ctx.setBeanResolver(new BeanFactoryResolver(applicationContext));
        if (result != null) ctx.setVariable("result", result);
        Expression compiled = SPEL_PARSER.parseExpression(expression);
        return compiled.getValue(ctx);
    }
}
