package org.zeniot.api;

import org.zeniot.dto.account.Account;
import org.zeniot.dto.core.PageQuery;
import org.zeniot.dto.core.PageResponse;

/**
 * @author Wu.Chunyang
 */
public interface AccountService {
    Account registerAccount(Account account);

    void deleteAccount(Long id);

    Account findById(Long accountId);

    PageResponse<Account> pageAll(PageQuery pageQuery);

    Account findByUsername(String username);
}
