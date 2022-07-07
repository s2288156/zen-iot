package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zeniot.server.controller.response.Account;
import org.zeniot.server.controller.response.RestResponse;
import org.zeniot.server.service.AccountService;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api/account")
@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    @ResponseBody
    @PostMapping("/register")
    public RestResponse registerAccount(@RequestBody Account account) {
        accountService.registerAccount(account);
        return RestResponse.ok();
    }

    @DeleteMapping("/{id}")
    public RestResponse deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return RestResponse.ok();
    }

}
