package org.zeniot.server.security.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_HEADER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isNotBlank(tokenHeader) && StringUtils.startsWith(tokenHeader, TOKEN_HEADER_PREFIX)) {
            String token = StringUtils.substring(tokenHeader, TOKEN_HEADER_PREFIX.length());

        }
    }
}
