package org.zeniot.transport.etherip.nio.protocol;

import org.zeniot.transport.etherip.Command;
import org.zeniot.transport.etherip.nio.Encapsulation;

/**
 * @author Jack Wu
 */
public class RegisterSession extends Encapsulation
{
    public RegisterSession()
    {
        super(Command.RegisterSession, 0,
                new RegisterSessionProtocol());
    }
}
