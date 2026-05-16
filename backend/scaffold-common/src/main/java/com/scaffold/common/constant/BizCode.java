package com.scaffold.common.constant;

/**
 * 通用业务错误码枚举。
 * <p>
 * 命名空间约定：{@code <子系统>_<场景>}。前端 / 移动端可基于 {@link #errorKey()} 做友好提示、
 * 节流提示、跳转引导。新增枚举值时务必保留稳定的 errorKey，{@link #defaultMessage()} 可以被
 * i18n 文件覆盖（messages_zh-CN.properties / messages_en-US.properties）。
 *
 * @author scaffold
 */
public enum BizCode implements ErrorCode
{
    SUCCESS(HttpStatus.SUCCESS, "BIZ_SUCCESS", "操作成功"),

    PARAM_INVALID(HttpStatus.BAD_REQUEST, "BIZ_PARAM_INVALID", "参数校验失败"),
    PARAM_MISSING(HttpStatus.BAD_REQUEST, "BIZ_PARAM_MISSING", "缺少必要参数"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "BIZ_UNAUTHORIZED", "登录已过期，请重新登录"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "BIZ_FORBIDDEN", "没有权限，请联系管理员授权"),
    DEMO_MODE(HttpStatus.FORBIDDEN, "BIZ_DEMO_MODE", "演示模式，不允许操作"),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "BIZ_RESOURCE_NOT_FOUND", "资源不存在"),
    METHOD_NOT_ALLOWED(HttpStatus.BAD_METHOD, "BIZ_METHOD_NOT_ALLOWED", "不允许的请求方式"),
    UNSUPPORTED_MEDIA(HttpStatus.UNSUPPORTED_TYPE, "BIZ_UNSUPPORTED_MEDIA", "不支持的数据类型"),

    CONFLICT(HttpStatus.CONFLICT, "BIZ_CONFLICT", "资源冲突或被锁定"),
    DUPLICATE_SUBMIT(HttpStatus.CONFLICT, "BIZ_DUPLICATE_SUBMIT", "操作正在处理中，请勿重复提交"),
    RATE_LIMITED(429, "BIZ_RATE_LIMITED", "访问过于频繁，请稍后再试"),

    INTERNAL_ERROR(HttpStatus.ERROR, "BIZ_INTERNAL_ERROR", "系统内部错误"),
    DEPENDENCY_UNAVAILABLE(503, "BIZ_DEPENDENCY_UNAVAILABLE", "下游依赖暂不可用"),

    BUSINESS_ERROR(HttpStatus.ERROR, "BIZ_BUSINESS_ERROR", "业务处理失败");

    private final int httpStatus;
    private final String errorKey;
    private final String defaultMessage;

    BizCode(int httpStatus, String errorKey, String defaultMessage)
    {
        this.httpStatus = httpStatus;
        this.errorKey = errorKey;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public int httpStatus()
    {
        return httpStatus;
    }

    @Override
    public String errorKey()
    {
        return errorKey;
    }

    @Override
    public String defaultMessage()
    {
        return defaultMessage;
    }
}
