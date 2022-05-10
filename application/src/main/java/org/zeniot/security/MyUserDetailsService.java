package org.zeniot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.zeniot.model.Account;
import org.zeniot.model.Role;
import org.zeniot.repository.AccountRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Wu.Chunyang
 */
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Account> accountOptional = accountRepository.findAccountByUsername(username);
        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            Set<Role> roles = account.getRoles();
            List<SimpleGrantedAuthority> authorities= roles.stream().map(rule -> new SimpleGrantedAuthority(rule.getName()))
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
