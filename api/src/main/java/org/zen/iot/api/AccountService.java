package org.zen.iot.api;

import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.account.Account;

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
