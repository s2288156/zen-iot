package org.zeniot.transport.etherip.nio.protocol;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public class RegisterSessionProtocol extends ProtocolAdapter {
    @Override
    public int getRequestSize()
    {
        return 4;
    }

    @Override
    public void encode(final ByteBuffer buf)
    {
        buf.putShort((short) 1); // protocol
        buf.putShort((short) 0); // options
    }
}
