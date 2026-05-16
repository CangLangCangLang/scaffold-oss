package com.scaffold.common.constant;

/**
 * 错误码契约。所有抛向前端的业务错误都建议实现此接口，
 * 由 {@link com.scaffold.common.exception.BizException} 与全局异常处理器自动透出。
 *
 * <p>{@link #errorKey()} 是稳定的、机器可读的错误标识（如 {@code BIZ_USER_NOT_FOUND}）。
 * 前端可以基于该 key 触发自定义提示、重试、跳转等行为，相比对照 HTTP 状态码或本地化文案更稳定。
 *
 * @author scaffold
 */
public interface ErrorCode
{
    /**
     * 业务码（与 {@link HttpStatus} 兼容，例如 401 / 403 / 500）。
     */
    int httpStatus();

    /**
     * 稳定的字符串错误标识，用于前端判定与多语言文案查表。
     * 形式：{@code <模块>_<场景>}，全大写，下划线分隔。
     */
    String errorKey();

    /**
     * 默认（中文）提示语。
     */
    String defaultMessage();
}
