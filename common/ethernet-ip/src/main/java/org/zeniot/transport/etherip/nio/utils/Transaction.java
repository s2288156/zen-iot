package org.zeniot.transport.etherip.nio.utils;

/**
 * @author Jack Wu
 */
public class Transaction {
    // SYNC on access
    private static long transaction;
    static long nextTransaction()
    {
        synchronized(Transaction.class)
        {
            ++transaction;
            if (transaction > 0xffffffffL)
                transaction = 1;
            return transaction;
        }
    }

    static byte[] format(final long transaction)
    {
        return String.format("%08X", transaction).getBytes();
    }

    static long parse(final byte[] bytes)
    {
        return -1;
    }
}
