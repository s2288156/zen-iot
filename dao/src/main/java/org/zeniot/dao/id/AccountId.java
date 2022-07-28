package org.zeniot.dao.id;

import lombok.NoArgsConstructor;

/**
 * @author Wu.Chunyang
 */
@NoArgsConstructor
public class AccountId extends AbstractId {

    private AccountId(Long id) {
        super(id);
    }

    public static AccountId of(Long id) {
        return new AccountId(id);
    }
}
