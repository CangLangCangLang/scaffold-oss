package com.scaffold.framework.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.scaffold.common.constant.BizCode;
import com.scaffold.common.constant.ErrorCode;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.DemoModeException;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.exception.user.CaptchaException;
import com.scaffold.common.exception.user.CaptchaExpireException;
import com.scaffold.common.exception.user.UserException;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.common.utils.html.EscapeUtil;

/**
 * 全局异常处理器：
 * <p>
 * 统一把异常映射到 {@link BizCode}，输出体携带 {@code errorKey}，前端可基于稳定 key 做提示与跳转。
 * 同时通过 {@link MessageSource} 解析以 errorKey 为 message-key 的 i18n 文案，找不到则回退默认消息。
 *
 * @author scaffold
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    /**
     * 结构化业务异常：优先消费此分支。
     */
    @ExceptionHandler(BizException.class)
    public AjaxResult handleBizException(BizException e, HttpServletRequest request)
    {
        log.warn("业务异常 path={} key={} msg={}", request.getRequestURI(), e.getErrorCode().errorKey(), e.getMessage());
        return localized(e.getErrorCode(), e.getMessage(), e.getArgs());
    }

    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        log.error("请求地址'{}',权限校验失败'{}'", request.getRequestURI(), e.getMessage());
        return localized(BizCode.FORBIDDEN, null);
    }

    /**
     * 登录验证码错误 / 过期属于用户输入问题，不应落到兜底 RuntimeException 变成内部错误。
     */
    @ExceptionHandler({CaptchaException.class, CaptchaExpireException.class})
    public AjaxResult handleCaptchaException(UserException e, HttpServletRequest request)
    {
        log.warn("登录验证码校验失败 path={} msg={}", request.getRequestURI(), e.getMessage());
        return AjaxResult.error(BizCode.PARAM_INVALID, e.getMessage());
    }

    /**
     * 兼容旧登录链路里的用户类异常（用户名密码错误、账号禁用等）。
     */
    @ExceptionHandler(UserException.class)
    public AjaxResult handleUserException(UserException e, HttpServletRequest request)
    {
        log.warn("用户认证异常 path={} msg={}", request.getRequestURI(), e.getMessage());
        return AjaxResult.error(BizCode.BUSINESS_ERROR, e.getMessage());
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request)
    {
        log.error("请求地址'{}',不支持'{}'请求", request.getRequestURI(), e.getMethod());
        return localized(BizCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    /**
     * 业务异常（旧 API，保持向下兼容）
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        // ServiceException 没有 errorKey，统一打到 BUSINESS_ERROR
        AjaxResult result = AjaxResult.error(BizCode.BUSINESS_ERROR, e.getMessage());
        if (code != null)
        {
            result.put(AjaxResult.CODE_TAG, code);
        }
        return result;
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", request.getRequestURI(), e);
        return AjaxResult.error(BizCode.PARAM_MISSING,
                String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
            HttpServletRequest request)
    {
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", request.getRequestURI(), e);
        return AjaxResult.error(BizCode.PARAM_INVALID,
                String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'",
                        e.getName(), e.getRequiredType() == null ? "?" : e.getRequiredType().getName(), value));
    }

    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        log.error("请求地址'{}',发生未知异常.", request.getRequestURI(), e);
        return localized(BizCode.INTERNAL_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        log.error("请求地址'{}',发生系统异常.", request.getRequestURI(), e);
        return localized(BizCode.INTERNAL_ERROR, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.warn("参数校验失败: {}", e.getMessage());
        String message = e.getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? error.getCode() : error.getDefaultMessage())
                .filter(StringUtils::isNotEmpty)
                .collect(java.util.stream.Collectors.joining("; "));
        return AjaxResult.error(BizCode.PARAM_INVALID, StringUtils.isEmpty(message) ? null : message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.warn("参数校验失败: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    String defaultMessage = fieldError.getDefaultMessage();
                    return fieldError.getField() + (StringUtils.isEmpty(defaultMessage) ? "" : (" " + defaultMessage));
                })
                .collect(java.util.stream.Collectors.joining("; "));
        return AjaxResult.error(BizCode.PARAM_INVALID, StringUtils.isEmpty(message) ? null : message);
    }

    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return localized(BizCode.DEMO_MODE, null);
    }

    private AjaxResult localized(ErrorCode errorCode, String fallbackMessage, Object... args)
    {
        String message;
        try
        {
            message = messageSource.getMessage(
                    errorCode.errorKey(),
                    args == null ? new Object[0] : args,
                    LocaleContextHolder.getLocale());
        }
        catch (NoSuchMessageException ignore)
        {
            message = StringUtils.isNotEmpty(fallbackMessage) ? fallbackMessage : errorCode.defaultMessage();
        }
        return AjaxResult.error(errorCode, message);
    }
}
