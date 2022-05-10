package org.zeniot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.zeniot.model.Account;
import org.zeniot.repository.AccountRepository;

import java.util.Optional;

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
            return User.builder()
                    .username(account.getUsername())
                    .password(account.getPassword())
                    .authorities("ADMIN")
                    .build();
        }
        return null;
    }
}
