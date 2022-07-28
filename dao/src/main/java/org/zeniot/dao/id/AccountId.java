package org.zeniot.dao.id;

/**
 * @author Wu.Chunyang
 */
public class AccountId extends AbstractId{
    private AccountId(Long id) {
        super(id);
    }
    public static AccountId of(Long id) {
        return new AccountId(id);
    }
}
