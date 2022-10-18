package org.zeniot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zeniot.common.exception.BizException;
import org.zeniot.data.SingleResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Wu.Chunyang
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BizException.class)
    public void handle(BizException bizException, HttpServletResponse response) throws IOException {
        SingleResponse<Object> ok = SingleResponse.ok(bizException.getMessage());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().println(new ObjectMapper().writeValueAsString(ok));
    }
}
