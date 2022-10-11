package org.zeniot.service.api;

import org.zeniot.dao.id.AccountId;
import org.zeniot.service.dto.account.Account;
import org.zeniot.service.dto.core.PageQuery;
import org.zeniot.service.dto.core.PageResponse;

/**
 * @author Wu.Chunyang
 */
public interface AccountService {
    Account registerAccount(Account account);

    void deleteAccount(Long id);

    Account findById(AccountId accountId);

    PageResponse<Account> pageAll(PageQuery pageQuery);
}
