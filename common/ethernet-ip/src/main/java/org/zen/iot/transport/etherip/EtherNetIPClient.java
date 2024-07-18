package org.zen.iot.transport.etherip;

import lombok.extern.slf4j.Slf4j;
import org.zen.iot.transport.etherip.nio.core.Connection;
import org.zen.iot.transport.etherip.nio.core.Encapsulation;
import org.zen.iot.transport.etherip.nio.core.TcpConnection;
import org.zen.iot.transport.etherip.nio.protocol.*;
import org.zen.iot.transport.etherip.nio.types.Command;

/**
 * @author Jack Wu
 */
@Slf4j
public class EtherNetIPClient implements AutoCloseable {

    private final String ip;
    private final int slot;
    private Connection connection = null;

    public EtherNetIPClient(String ip, int slot) {
        this.ip = ip;
        this.slot = slot;
    }

    @Override
    public void close() throws Exception {
        if (this.connection != null) {
            this.unregisterSession();
            this.connection.close();
        }
    }

    /**
     * Connect to device via TCP, register session
     */
    public void connectTcp() throws Exception {
        this.connection = new TcpConnection(this.ip, this.slot);
        this.registerSession();
    }

    /**
     * Read a single array tag
     *
     * @param tag   Name of tag
     * @param count Number of array elements to read
     * @return Current value of the tag
     * @throws Exception on error
     */
    public CIPData readTag(final String tag, final short count) throws Exception {
        final MRChipReadProtocol cip_read = new MRChipReadProtocol(tag, count);
        final Encapsulation encap =
                new Encapsulation(Command.SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(this.slot, cip_read)));
        this.connection.execute(encap);

        return cip_read.getData();
    }

    /**
     * Write a tag
     *
     * @param tag   Tag name
     * @param value Value to write
     * @throws Exception on error
     */
    public void writeTag(final String tag, final CIPData value) throws Exception {
        final MRChipWriteProtocol cip_write = new MRChipWriteProtocol(tag, value);

        final Encapsulation encap =
                new Encapsulation(Command.SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(this.slot,
                                        cip_write)));
        this.connection.execute(encap);
    }

    /**
     * Register session
     */
    private void registerSession() throws Exception {
        final RegisterSession register = new RegisterSession();
        this.connection.execute(register);
        this.connection.setSession(register.getSession());
    }

    /**
     * Unregister session (device will close connection)
     */
    private void unregisterSession() {
        try {
            if (this.connection.getSession() == 0 || !this.connection.isOpen()) {
                return;
            }
            this.connection.write(new Encapsulation(Command.UnRegisterSession,
                    this.connection.getSession(), new ProtocolAdapter()));
            // Cannot read after this point because PLC will close the connection
        } catch (final Exception ex) {

        }
    }
}
