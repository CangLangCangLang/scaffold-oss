package com.scaffold.common.exception;

import com.scaffold.common.constant.BizCode;
import com.scaffold.common.constant.ErrorCode;

/**
 * 结构化业务异常。携带 {@link ErrorCode}（推荐基于 {@link BizCode}），
 * 由全局异常处理器自动转换为 {@code AjaxResult / R} 并附带 {@code errorKey}。
 *
 * @author scaffold
 */
public final class BizException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /** 用于占位符替换的参数（例如 messages.properties 中的 {0} {1}） */
    private final Object[] args;

    public BizException(ErrorCode errorCode)
    {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public BizException(ErrorCode errorCode, String detail)
    {
        super(detail);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public BizException(ErrorCode errorCode, Object... args)
    {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }

    public BizException(ErrorCode errorCode, Throwable cause)
    {
        super(errorCode.defaultMessage(), cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public ErrorCode getErrorCode()
    {
        return errorCode;
    }

    public Object[] getArgs()
    {
        return args;
    }
}
