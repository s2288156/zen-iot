package org.zeniot.server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.zeniot.api.AccountService;
import org.zeniot.data.domain.account.Account;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Wu.Chunyang
 */
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountService accountService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountService.findByUsername(username);
        if (account != null) {
            List<SimpleGrantedAuthority> authorities = account.getRoles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            return User.builder()
                    .username(account.getUsername())
                    .password(account.getPassword())
                    .authorities(authorities)
                    .build();
        }
        return null;
    }
}
