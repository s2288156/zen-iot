package org.zeniot.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.zeniot.security.MyAuthenticationSuccessHandler;

/**
 * @author Wu.Chunyang
 */
@EnableWebSecurity
public class SecurityWebConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .anyRequest().authenticated()
                .and()
                .formLogin();
//        http.headers().cacheControl()
//                .and()
//                .frameOptions().disable()
//                .and()
//                .cors()
//                .and()
//                .csrf().disable()
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
//                .formLogin()
//                .successHandler(new MyAuthenticationSuccessHandler());
    }
}
