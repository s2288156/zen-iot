package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zeniot.api.AccountService;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.base.SingleResponse;
import org.zeniot.data.domain.account.Account;
import org.zeniot.server.security.JwtHandler;
import org.zeniot.data.vo.UserToken;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class AccountController extends AbstractController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtHandler jwtHandler;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public SingleResponse<UserToken> login(@RequestBody Account account) {
        UserDetails user = userDetailsService.loadUserByUsername(account.getUsername());
        if (user == null) {
            return SingleResponse.failure("用户名不存在");
        }
        boolean matches = passwordEncoder.matches(account.getPassword(), user.getPassword());
        if (matches) {
            String token = jwtHandler.newToken(user);
            return SingleResponse.success(new UserToken(token));
        }
        return SingleResponse.failure("Login failed.");
    }

    @PostMapping("/account/register")
    public SingleResponse<Account> registerAccount(@Validated @RequestBody Account account) {
        return SingleResponse.success(accountService.registerAccount(account));
    }

    //    @PreAuthorize("hasAnyAuthority(sd.role.ADMIN)")
    @DeleteMapping("/account/{id}")
    public SingleResponse<Account> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return SingleResponse.success();
    }

    @GetMapping("/account/{id}")
    public SingleResponse<Account> findAccountById(@PathVariable Long id) {
        Account account = accountService.findById(id);
        return SingleResponse.success(account);
    }

    @GetMapping("/accounts")
    public PageResponse<Account> findAccounts(@Validated PageQuery query) {
        return accountService.pageAll(query);
    }

}
