package com.scaffold.common.core.domain;

import java.io.Serializable;
import com.scaffold.common.constant.HttpStatus;
import com.scaffold.common.core.trace.TraceContext;

/**
 * 响应信息主体。
 * <p>
 * 在原有 code/msg/data 之外增加 {@code traceId}，便于前端定位、链路联查。
 * traceId 默认从 {@link TraceContext} 读取（由 {@code TraceIdFilter} 写入 MDC），
 * 不需要业务代码显式传入。
 *
 * @author scaffold
 */
public class R<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 成功 */
    public static final int SUCCESS = HttpStatus.SUCCESS;

    /** 失败 */
    public static final int FAIL = HttpStatus.ERROR;

    private int code;

    private String msg;

    private T data;

    private String traceId;

    /**
     * 结构化错误标识，与 {@link com.scaffold.common.constant.BizCode#errorKey()} 对齐，
     * 前端依赖该字段判定错误类型，比 {@link #code} / {@link #msg} 更稳定。
     */
    private String errorKey;

    public static <T> R<T> ok()
    {
        return restResult(null, SUCCESS, "操作成功", null);
    }

    public static <T> R<T> ok(T data)
    {
        return restResult(data, SUCCESS, "操作成功", null);
    }

    public static <T> R<T> ok(T data, String msg)
    {
        return restResult(data, SUCCESS, msg, null);
    }

    public static <T> R<T> fail()
    {
        return restResult(null, FAIL, "操作失败", null);
    }

    public static <T> R<T> fail(String msg)
    {
        return restResult(null, FAIL, msg, null);
    }

    public static <T> R<T> fail(T data)
    {
        return restResult(data, FAIL, "操作失败", null);
    }

    public static <T> R<T> fail(T data, String msg)
    {
        return restResult(data, FAIL, msg, null);
    }

    public static <T> R<T> fail(int code, String msg)
    {
        return restResult(null, code, msg, null);
    }

    public static <T> R<T> fail(int code, String msg, String errorKey)
    {
        return restResult(null, code, msg, errorKey);
    }

    private static <T> R<T> restResult(T data, int code, String msg, String errorKey)
    {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        apiResult.setTraceId(TraceContext.getTraceId());
        apiResult.setErrorKey(errorKey);
        return apiResult;
    }

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public T getData()
    {
        return data;
    }

    public void setData(T data)
    {
        this.data = data;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getErrorKey()
    {
        return errorKey;
    }

    public void setErrorKey(String errorKey)
    {
        this.errorKey = errorKey;
    }

    public static <T> Boolean isError(R<T> ret)
    {
        return !isSuccess(ret);
    }

    public static <T> Boolean isSuccess(R<T> ret)
    {
        return R.SUCCESS == ret.getCode();
    }
}
