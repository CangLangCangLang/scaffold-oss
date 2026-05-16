package com.scaffold.framework.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import com.scaffold.common.annotation.Idempotent;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.common.utils.StringUtils;

/**
 * 幂等切面：基于 Redis SET NX EX 的分布式幂等控制。命中已存在键即认为是重复请求。
 *
 * @author scaffold
 */
@Aspect
@Component
public class IdempotentAspect
{
    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private static final String KEY_PREFIX = "idempotent:";

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable
    {
        String redisKey = buildKey(point, idempotent);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", idempotent.expire(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired))
        {
            log.warn("幂等命中重复请求 key={}", redisKey);
            throw new ServiceException(idempotent.message());
        }
        try
        {
            return point.proceed();
        }
        catch (Throwable t)
        {
            stringRedisTemplate.delete(redisKey);
            throw t;
        }
    }

    private String buildKey(ProceedingJoinPoint point, Idempotent idempotent)
    {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();

        String resolvedKey;
        if (StringUtils.isNotEmpty(idempotent.key()))
        {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    point.getTarget(), method, args, parameterNameDiscoverer);
            try
            {
                Expression expression = parser.parseExpression(idempotent.key());
                Object value = expression.getValue(context);
                resolvedKey = value == null ? "null" : value.toString();
            }
            catch (Exception e)
            {
                log.warn("解析幂等 SpEL 失败，回退到默认 key 策略: {}", e.getMessage());
                resolvedKey = fallbackKey(method, args);
            }
        }
        else
        {
            resolvedKey = fallbackKey(method, args);
        }
        return KEY_PREFIX + idempotent.prefix() + ":" + resolvedKey;
    }

    private String fallbackKey(Method method, Object[] args)
    {
        String username = "anonymous";
        try
        {
            LoginUser user = SecurityUtils.getLoginUser();
            if (user != null && StringUtils.isNotEmpty(user.getUsername()))
            {
                username = user.getUsername();
            }
        }
        catch (Exception ignored)
        {
        }
        int argsHash = args == null ? 0 : java.util.Arrays.deepHashCode(args);
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + ":" + username + ":" + argsHash;
    }
}
