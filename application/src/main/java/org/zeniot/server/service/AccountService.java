package org.zeniot.server.service;

import org.zeniot.dao.id.AccountId;
import org.zeniot.server.dto.PageQuery;
import org.zeniot.server.dto.account.Account;
import org.zeniot.server.controller.response.PageResponse;

/**
 * @author Wu.Chunyang
 */
public interface AccountService {
    Account registerAccount(Account account);

    void deleteAccount(Long id);

    Account findById(AccountId accountId);

    PageResponse<Account> pageAll(PageQuery pageQuery);
}
