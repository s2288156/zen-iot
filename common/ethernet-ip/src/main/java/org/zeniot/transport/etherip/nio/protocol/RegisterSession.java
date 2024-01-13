package org.zeniot.transport.etherip.nio.protocol;

import org.zeniot.transport.etherip.nio.core.Encapsulation;
import org.zeniot.transport.etherip.nio.types.Command;

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
