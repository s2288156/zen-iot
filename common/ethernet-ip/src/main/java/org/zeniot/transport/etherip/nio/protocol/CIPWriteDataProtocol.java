package org.zeniot.transport.etherip.nio.protocol;

import java.nio.ByteBuffer;


/**
 * Protocol body for {@link org.zeniot.transport.etherip.nio.types.CNService#CIP_WriteData}
 *
 * @author Jack Wu
 */
public class CIPWriteDataProtocol extends ProtocolAdapter {
    final private CIPData data;

    public CIPWriteDataProtocol(final CIPData data) {
        this.data = data;
    }

    @Override
    public int getRequestSize() {
        return this.data.getEncodedSize();
    }

    @Override
    public void encode(final ByteBuffer buf)
            throws Exception {
        this.data.encode(buf);
    }
}
