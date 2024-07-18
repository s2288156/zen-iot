package org.zen.iot.transport.etherip.nio.protocol;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public class CIPReadDataProtocol extends ProtocolAdapter {
    private CIPData data;
    private final short count;

    /**
     * Create a read protocol message that requests a single element
     */
    public CIPReadDataProtocol() {
        this.count = 1;
    }

    /**
     * Create a read protocol message that requests one or more elements if request is an array
     *
     * @param count
     */
    public CIPReadDataProtocol(final short count) {
        this.count = count;
    }

    @Override
    public int getRequestSize() {
        return 2;
    }

    @Override
    public void encode(final ByteBuffer buf) {
        buf.putShort(this.count); // elements
    }

    @Override
    public void decode(final ByteBuffer buf, final int available) throws Exception {
        if (available <= 0) {
            this.data = null;
            return;
        }
        final CIPData.Type type = CIPData.Type.forCode(buf.getShort());
        final byte[] raw = new byte[available - 2];
        buf.get(raw);
        this.data = new CIPData(type, raw);
    }

    final public CIPData getData() {
        return this.data;
    }
}
