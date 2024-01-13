package org.zeniot.transport.etherip.nio.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.zeniot.transport.etherip.nio.protocol.Protocol;
import org.zeniot.transport.etherip.nio.protocol.ProtocolDecoder;
import org.zeniot.transport.etherip.nio.protocol.ProtocolEncoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Jack Wu
 */
@Slf4j
public abstract class Connection implements AutoCloseable {

    final public static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    final private static int BUFFER_SIZE = 600;

    protected final int slot;

    protected final ByteBuffer buffer;

    @Setter
    @Getter
    private int session = 0;

    /**
     * Timeout for individual read/write operation
     */
    protected long timeout_ms = 2000;

    /**
     * TCP connection will apply 'timeout_ms' to individual read,
     * and this retry count to overall read of one message
     */
    protected long max_retry_count = 10;

    protected int port = 44818;

    /**
     * Initialize
     *
     * @param address IP address of device
     * @param slot    Slot number 0, 1, .. of the controller within PLC crate
     * @throws Exception on error
     */
    public Connection(final String address, final int slot) throws Exception {
        log.info("Connection to {}:{}", address, port);
        this.slot = slot;
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.buffer.order(BYTE_ORDER);
    }

    public abstract boolean isOpen() throws Exception;

    /**
     * Write protocol data
     *
     * @param encoder {@link ProtocolEncoder} used to <code>encode</code> buffer
     * @throws Exception on error
     */
    public abstract void write(final ProtocolEncoder encoder) throws Exception;

    /**
     * Read protocol data
     *
     * @param decoder {@link ProtocolDecoder} used to <code>decode</code> buffer
     * @throws Exception on error
     */
    protected abstract void read(final ProtocolDecoder decoder) throws Exception;

    /**
     * Write protocol request and handle response
     *
     * @param protocol {@link Protocol}
     * @throws Exception on error
     */
    public synchronized void execute(final Protocol protocol) throws Exception {
        this.write(protocol);
        this.read(protocol);
    }
}
