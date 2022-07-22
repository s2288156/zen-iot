package org.zeniot.server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;
import org.zeniot.dao.repository.AccountRepository;

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
        Optional<AccountEntity> accountOptional = accountRepository.findAccountByUsername(username);
        if (accountOptional.isPresent()) {
            AccountEntity accountEntity = accountOptional.get();
            Set<RoleEntity> roleEntities = accountEntity.getRoles();
            List<SimpleGrantedAuthority> authorities= roleEntities.stream().map(rule -> new SimpleGrantedAuthority(rule.getRoleName()))
                    .collect(Collectors.toList());
            return User.builder()
                    .username(accountEntity.getUsername())
                    .password(accountEntity.getPwd())
                    .authorities(authorities)
                    .build();
        }
        return null;
    }
}
