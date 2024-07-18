package org.zen.iot.transport.etherip.nio.core;

import lombok.extern.slf4j.Slf4j;
import org.zen.iot.transport.etherip.nio.protocol.ProtocolDecoder;
import org.zen.iot.transport.etherip.nio.protocol.ProtocolEncoder;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Jack Wu
 */
@Slf4j
public class TcpConnection extends Connection {

    private final AsynchronousSocketChannel channel;

    /**
     * Initialize
     *
     * @param address IP address of device
     * @param slot    Slot number 0, 1, .. of the controller within PLC crate
     * @throws Exception
     */
    public TcpConnection(final String address, final int slot) throws Exception {
        super(address, slot);

        this.channel = AsynchronousSocketChannel.open();
        this.channel.connect(new InetSocketAddress(address, this.port))
                .get(this.timeout_ms, MILLISECONDS);
    }

    @Override
    public boolean isOpen() throws Exception {
        return this.channel.isOpen();
    }

    @Override
    public void write(ProtocolEncoder encoder) throws Exception {
        this.buffer.clear();
        encoder.encode(this.buffer);

        this.buffer.flip();

        int to_write = this.buffer.limit();
        while (to_write > 0) {
            final int written = this.channel.write(this.buffer).get(this.timeout_ms, MILLISECONDS);
            to_write -= written;
            if (to_write > 0) {
                this.buffer.compact();
            }
        }
    }

    @Override
    protected void read(ProtocolDecoder decoder) throws Exception {
        // Read until protocol has enough data to decode
        this.buffer.clear();
        long count = 0;
        do {
            if (++count > max_retry_count)
                throw new TimeoutException("Message response time out");
            this.channel.read(this.buffer).get(this.timeout_ms, MILLISECONDS);
        } while (this.buffer.position() < decoder.getResponseSize(this.buffer));

        // Prepare to decode
        this.buffer.flip();

        try {
            decoder.decode(this.buffer, this.buffer.remaining());
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public void close() throws Exception {
        this.channel.close();
    }
}
