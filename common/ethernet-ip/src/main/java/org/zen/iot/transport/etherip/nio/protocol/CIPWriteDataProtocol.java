package org.zen.iot.transport.etherip.nio.protocol;

import org.zen.iot.transport.etherip.nio.types.CNService;

import java.nio.ByteBuffer;


/**
 * Protocol body for {@link CNService#CIP_WriteData}
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
