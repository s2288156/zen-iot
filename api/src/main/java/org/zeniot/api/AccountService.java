package org.zeniot.api;

import org.zeniot.data.PageQuery;
import org.zeniot.data.PageResponse;
import org.zeniot.dto.account.Account;

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
