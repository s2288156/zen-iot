package com.zen.common.core.web;

import com.zen.common.core.api.ApiResponse;
import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器:将未捕获异常统一转换为 {@link ApiResponse}。
 *
 * <p>HTTP 状态与响应体 code 对齐;code 非法/非 HTTP 语义(如业务码段 1xxx)时 HTTP 回落 200,由 body 的 code 表达错误。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return build(e.getErrorCode().getCode(), e.getMessage());
    }

    /** MethodArgumentNotValidException(@RequestBody 校验失败)在 Spring 7 中继承 BindException,一并覆盖。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(GlobalErrorCode.BAD_REQUEST.getMessage());
        log.warn("参数绑定校验失败: {}", message);
        return build(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(GlobalErrorCode.BAD_REQUEST.getMessage());
        log.warn("方法参数校验失败: {}", message);
        return build(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /** Spring 7 新增父类,统一覆盖缺失 request param/header/cookie 等情况。 */
    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestValue(MissingRequestValueException e) {
        log.warn("缺少必需的请求参数: {}", e.getMessage());
        return build(GlobalErrorCode.BAD_REQUEST.getCode(), GlobalErrorCode.BAD_REQUEST.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return build(GlobalErrorCode.METHOD_NOT_ALLOWED.getCode(), GlobalErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    /** Spring 6/7 未匹配任何 Controller 的路径抛此异常,须透为 404,避免被 Exception 兜底误判为 500。 */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ServletException e) {
        log.warn("请求资源不存在: {}", e.getMessage());
        return build(GlobalErrorCode.NOT_FOUND.getCode(), GlobalErrorCode.NOT_FOUND.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("未处理异常", e);
        return build(
                GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        HttpStatus responseStatus = (status != null && status.isError()) ? status : HttpStatus.OK;
        return ResponseEntity.status(responseStatus).body(ApiResponse.failure(code, message));
    }
}
