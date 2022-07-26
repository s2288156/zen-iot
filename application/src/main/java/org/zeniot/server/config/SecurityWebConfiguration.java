package org.zeniot.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.zeniot.server.security.LoginFilter;
import org.zeniot.server.security.component.RestAuthenticationEntryPoint;
import org.zeniot.server.security.handler.RestAuthenticationFailureHandler;
import org.zeniot.server.security.handler.RestAuthenticationSuccessHandler;

/**
 * @author Wu.Chunyang
 */
@EnableWebSecurity
@Configuration
public class SecurityWebConfiguration {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RestAuthenticationSuccessHandler successHandler;
    @Autowired
    private RestAuthenticationFailureHandler failureHandler;
    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .and()
                .authorizeRequests()
//                .antMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint())
                .and()
                .csrf().disable()
                .cors().disable()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.userDetailsService(userDetailsService);
//        http.addFilterAt(loginFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public AuthenticationManager authenticationManager() throws Exception {
//        return authenticationConfiguration.getAuthenticationManager();
//    }
//
//    @Bean
//    public LoginFilter loginFilter() throws Exception {
//        LoginFilter loginFilter = new LoginFilter();
//        loginFilter.setAuthenticationManager(authenticationManager());
//        loginFilter.setAuthenticationSuccessHandler(successHandler);
//        loginFilter.setAuthenticationFailureHandler(failureHandler);
//        return loginFilter;
//    }

}
