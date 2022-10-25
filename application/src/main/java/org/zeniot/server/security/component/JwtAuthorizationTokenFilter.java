package org.zeniot.server.security.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zeniot.common.exception.BizException;
import org.zeniot.data.Response;
import org.zeniot.server.security.JwtHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Wu.Chunyang
 */
@Component
@Slf4j
public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtHandler jwtHandler;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_HEADER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader(TOKEN_HEADER);
        log.warn("method: {}, token: {}", request.getMethod(), tokenHeader);
        if (!HttpMethod.OPTIONS.matches(request.getMethod()) && StringUtils.isNotBlank(tokenHeader) && StringUtils.startsWith(tokenHeader, TOKEN_HEADER_PREFIX)) {
            String token = StringUtils.substring(tokenHeader, TOKEN_HEADER_PREFIX.length());
            User user = jwtHandler.getUserForToken(token);
            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    if (jwtHandler.verifyToken(token, user)) {
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                } catch (BizException e) {
                    Response failureResponse = Response.failure(e);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().println(new ObjectMapper().writeValueAsString(failureResponse));
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
