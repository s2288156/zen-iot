package org.zen.iot.transport.etherip.nio.protocol;


import org.zen.iot.transport.etherip.nio.types.CNPath;
import org.zen.iot.transport.etherip.nio.types.CNService;

import java.nio.ByteBuffer;

/**
 * Protocol PDU that uses CM_Unconnected_Send
 *
 * @author Jack Wu
 */
public class UnconnectedSendProtocol extends ProtocolAdapter {
    final private ProtocolEncoder encoder;
    final private int slot;
    final private Protocol body;

    /**
     * Initialize
     *
     * @param slot Slot (0, 1, ...) of controller module in crate
     * @param body Embedded protocol for read/write
     */
    public UnconnectedSendProtocol(final int slot, final Protocol body) {
        this.encoder = new MessageRouterProtocol(CNService.CM_Unconnected_Send,
                CNPath.ConnectionManager(), new ProtocolAdapter());
        this.slot = slot;
        this.body = body;
    }

    @Override
    public int getRequestSize() {
        return this.encoder.getRequestSize() + 4 + this.body.getRequestSize()
                + (this.needPad() ? 1 : 0) + 4;
    }

    @Override
    public void encode(final ByteBuffer buf)
            throws Exception {
        this.encoder.encode(buf);

        final byte tick_time = (byte) 10;
        final byte ticks = (byte) 240;
        buf.put(tick_time);
        buf.put(ticks);
        final short body_size = (short) this.body.getRequestSize();
        buf.putShort(body_size);

        final boolean pad = this.needPad();

        this.body.encode(buf);
        if (pad) {
            buf.put((byte) 0);
        }

        buf.put((byte) 1); // Path size
        buf.put((byte) 0); // reserved
        buf.put((byte) 1); // Port 1 = backplane
        buf.put((byte) this.slot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decode(final ByteBuffer buf, final int available) throws Exception {
        // CM_Unconnected_Send wraps a request.
        // The response then arrives without CM_Unconnected_Send wrapper,
        // so pass decoding on to the body.
        this.body.decode(buf, available);
    }

    /**
     * @return Is path of odd length, requiring a pad byte?
     */
    private boolean needPad() {
        // Findbugs: x%2==1 fails for negative numbers
        return (this.body.getRequestSize() % 2) != 0;
    }
}
