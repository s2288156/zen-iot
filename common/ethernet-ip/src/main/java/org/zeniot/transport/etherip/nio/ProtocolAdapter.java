package org.zeniot.transport.etherip.nio;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public class ProtocolAdapter implements Protocol{
    @Override
    public int getResponseSize(ByteBuffer buf) throws Exception {
        return 0;
    }

    @Override
    public void decode(ByteBuffer buf, int available) throws Exception {

    }

    @Override
    public int getRequestSize() {
        return 0;
    }

    @Override
    public void encode(ByteBuffer buf) throws Exception {

    }
}
