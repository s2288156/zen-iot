package org.zen.iot.transport.etherip.nio.protocol;

import org.zen.iot.transport.etherip.nio.core.Encapsulation;
import org.zen.iot.transport.etherip.nio.types.Command;

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
