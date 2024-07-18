package org.zen.iot.server.controller;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.base.SingleResponse;
import org.zen.iot.data.domain.account.Account;
import org.zen.iot.data.vo.UserToken;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class AccountController extends AbstractController {

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
