package org.zeniot.server.service;

import org.zeniot.server.controller.response.Account;
import org.zeniot.server.controller.response.PageResponse;

/**
 * @author Wu.Chunyang
 */
public interface AccountService {
    void registerAccount(Account account);

    void deleteAccount(Long id);

    PageResponse<Account> pageAll();
}
