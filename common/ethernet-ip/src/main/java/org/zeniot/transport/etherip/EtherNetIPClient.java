package org.zeniot.transport.etherip;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.zeniot.transport.etherip.netty.TcpConnection;

/**
 * @author Jack Wu
 */
@Slf4j
public class EtherNetIPClient {

    private final String ip;
    private final int slot;

    private TcpConnection connection;

    public EtherNetIPClient(String ip, int slot) {
        this.ip = ip;
        this.slot = slot;
    }

    public void connectTcp() {
        this.connection = new TcpConnection();
        this.connection.connect(ip);
    }

    public void disconnect() {
        this.connection.close();
    }

    public void write() {
        final int status = 0;
        final int options = 0;
        ByteBuf req = ByteBufAllocator.DEFAULT.buffer();
        req.writeShort(Command.RegisterSession.getCode());
        req.writeShort(4);
        // session
        req.writeInt(0);
        req.writeInt(status);
        // Transaction
        req.writeBytes(String.format("%08X", 3).getBytes());
        req.writeInt(options);
        req.writeShort(1);
        req.writeShort(0);
        log.info("{}", req);
        this.connection.write(req);
    }
}
