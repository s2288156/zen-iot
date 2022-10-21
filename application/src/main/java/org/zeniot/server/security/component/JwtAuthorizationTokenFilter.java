package org.zeniot.server.security.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zeniot.server.security.JwtHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Wu.Chunyang
 */
@Component
@Slf4j
public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtHandler jwtHandler;
//    @Autowired
//    private UserDetailsService userDetailsService;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_HEADER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isNotBlank(tokenHeader) && StringUtils.startsWith(tokenHeader, TOKEN_HEADER_PREFIX)) {
            String token = StringUtils.substring(tokenHeader, TOKEN_HEADER_PREFIX.length());
            User user = jwtHandler.getUserForToken(token);
            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtHandler.verifyToken(token, user)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
//            String username = jwtHandler.getUsernameForToken(token);
//            if (StringUtils.isNotBlank(username) && SecurityContextHolder.getContext().getAuthentication() != null) {
//                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                if (jwtHandler.verifyToken(token, userDetails)) {
//                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//                }
//            }
        }
        filterChain.doFilter(request, response);
    }
}
