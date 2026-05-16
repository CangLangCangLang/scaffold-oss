package com.scaffold.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解（结构化事件审计，落 sys_audit_log 表）。
 * <p>
 * 与 {@link Log} 互补：
 * <ul>
 *   <li>{@link Log} 记录"谁在何时调了什么接口、传了什么参数"，是流水。</li>
 *   <li>{@code @AuditLog} 记录"谁对哪个资源做了什么变更，前后值是什么、差异如何"，是事件源。</li>
 * </ul>
 * 关键写操作建议同时挂 {@link Log} + {@code @AuditLog}。
 *
 * <h3>SpEL 支持</h3>
 * {@link #resourceId()} / {@link #beforeProvider()} / {@link #comment()} 都支持 SpEL，
 * 上下文为方法参数（按参数名 / 参数索引访问），还可注入：
 * <ul>
 *   <li>{@code #root.method} —— 当前方法</li>
 *   <li>{@code #root.target} —— 当前 bean</li>
 *   <li>{@code #result} —— 方法返回值（仅在 {@code beforeProvider} / {@code resourceId} 时为 null，在 success-after 解析 comment 时可用）</li>
 * </ul>
 * 失败时 SpEL 解析异常会被吞并 + warn 日志，不影响业务。
 *
 * @author scaffold
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog
{
    /**
     * 业务模块名，例如 {@code system.user}、{@code workflow.process}。
     * 建议层级用 {@code .} 分隔以便检索。
     */
    String module();

    /**
     * 动作名，自由字符串（不强制枚举），例如：
     * {@code CREATE / UPDATE / DELETE / APPROVE / REJECT / EXPORT / RESET_PASSWORD}。
     */
    String action();

    /** 资源类型，如 {@code user}、{@code role}、{@code processInstance}。可空。 */
    String resourceType() default "";

    /**
     * 资源 ID 的 SpEL 表达式，例如 {@code "#userId"} / {@code "#user.userId"} / {@code "#result.data.id"}。
     * 解析失败时记 null，不影响审计落库。
     */
    String resourceId() default "";

    /**
     * 业务侧自定义说明的 SpEL 表达式（可选）。
     * 例：{@code "'修改用户为 ' + #user.userName"}。
     */
    String comment() default "";

    /**
     * before 快照的 SpEL 表达式（可选）。例如 {@code "@sysUserService.selectUserById(#userId)"}。
     * 注意：表达式在方法**执行之前**求值；若需要从 DB 拿数据，
     * 建议表达式调用 service 查询。
     * 留空表示不记录 before（CREATE / 查询性动作）。
     */
    String beforeProvider() default "";

    /**
     * 是否把方法返回值作为 after 快照。
     * <ul>
     *   <li>true（默认）：取 controller 方法的返回对象。AjaxResult 等包装会被原样序列化（可在 {@link #excludeFields()} 中排掉无关字段）。</li>
     *   <li>false：不记录 after（如 DELETE 等没有有意义返回值的场景）。</li>
     * </ul>
     */
    boolean recordReturn() default true;

    /**
     * 在 {@code SensitiveJsonSerializer} / {@code @SensitiveLog} 之外，
     * 额外排除的 JSON 顶层字段名（使用 fastjson PropertyPreExcludeFilter）。
     * 例：排除 {@code "password"}（虽然该字段已在全局排除清单里，业务可附加更多）。
     */
    String[] excludeFields() default {};
}
