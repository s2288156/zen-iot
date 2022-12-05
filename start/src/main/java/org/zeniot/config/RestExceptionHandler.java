package org.zeniot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zeniot.common.exception.BizException;
import org.zeniot.data.base.SingleResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BizException.class)
    public void handle(BizException bizException, HttpServletResponse response) throws IOException {
        SingleResponse<Object> ok = SingleResponse.success(bizException.getMessage());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().println(new ObjectMapper().writeValueAsString(ok));
    }

    @ExceptionHandler(Exception.class)
    public void handleEx(Exception ex, HttpServletResponse response) throws IOException {
        log.error("未知异常: ", ex);
        SingleResponse<Object> failure = SingleResponse.failure(ex.getLocalizedMessage());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().println(new ObjectMapper().writeValueAsString(failure));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleMethodArgsValidEx(MethodArgumentNotValidException ex, HttpServletResponse response) throws IOException {
        String errorMsg = StringUtils.defaultString(ex.getFieldError().getDefaultMessage(), "参数校验异常!");
        SingleResponse<Object> failure = SingleResponse.failure(errorMsg);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().println(new ObjectMapper().writeValueAsString(failure));
    }
}
