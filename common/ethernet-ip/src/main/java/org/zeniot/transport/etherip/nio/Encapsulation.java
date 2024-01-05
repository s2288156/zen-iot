package org.zeniot.transport.etherip.nio;

import lombok.Getter;
import org.zeniot.transport.etherip.Command;
import org.zeniot.transport.etherip.nio.protocol.Protocol;
import org.zeniot.transport.etherip.nio.utils.Hexdump;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Jack Wu
 */
public class Encapsulation implements Protocol {
    /**
     * Byte size of encapsulation header
     */
    final public static int ENCAPSULATION_HEADER_SIZE = 24;
    private final byte[] context = Transaction.format(Transaction.nextTransaction());
    ;
    final private Command command;
    @Getter
    private int session;
    final private Protocol body;

    public Encapsulation(final Command command, final int session,
                         final Protocol body) {
        this.command = command;
        this.session = session;
        this.body = body;
    }

    @Override
    public int getResponseSize(ByteBuffer buf) throws Exception {
        // Need at least the encapsulation header
        final int needed = 24 - buf.position();
        if (needed > 0) {
            return needed;
        }

        // Buffer contains header (and maybe more after that)
        final short command_code = buf.getShort(0);
        final Command command = Command.forCode(command_code);
        if (command == null) {
            throw new Exception(
                    "Received unknown command code " + command_code);
        }
        if (command != this.command) {
            throw new Exception("Received command " + command + " instead of "
                    + this.command);
        }

        final short body_size = buf.getShort(2);

        return 24 + body_size;
    }

    @Override
    public void decode(ByteBuffer buf, int available) throws Exception {
        // Start decoding
        final short command_code = buf.getShort();
        final Command command = Command.forCode(command_code);
        if (command == null) {
            throw new Exception(
                    "Received unknown command code " + command_code);
        }
        if (command != this.command) {
            throw new Exception("Received command " + command + " instead of "
                    + this.command);
        }

        final short body_size = buf.getShort();

        final int session = buf.getInt();
        if (session != this.session) {
            // If we did not have a session, remember the newly obtained session
            if (this.session == 0) {
                this.session = session;
            } else {
                throw new Exception("Received session " + session
                        + " instead of " + this.session);
            }
        }

        final int status = buf.getInt();

        final byte[] recvd_context = new byte[8];
        buf.get(recvd_context);
        if (!Arrays.equals(recvd_context, context))
            throw new Exception("Received context " + Hexdump.toAscii(recvd_context) + ", expected " + Hexdump.toAscii(context));

        final int options = buf.getInt();

        if (status != 0) {
            throw new Exception(String.format("Received status 0x%08X (%s)\n",
                    status, this.getStatusMessage(status)));
        }
        if (buf.remaining() < body_size) {
            throw new Exception("Need " + body_size + " more bytes, have "
                    + buf.remaining());
        }

        this.body.decode(buf, body_size);
    }

    @Override
    public int getRequestSize() {
        return ENCAPSULATION_HEADER_SIZE + this.body.getRequestSize();
    }

    @Override
    public void encode(ByteBuffer buf) throws Exception {
        final int status = 0;
        final int options = 0;

        buf.putShort(this.command.getCode());
        buf.putShort((short) this.body.getRequestSize());
        buf.putInt(this.session);
        buf.putInt(status);
        buf.put(context);
        buf.putInt(options);

        this.body.encode(buf);
    }

    private String getStatusMessage(final int status) {
        switch (status) {
            case 0x00:
                return "OK";
            case 0x01:
                return "invalid/unsupported command";
            case 0x02:
                return "no memory on target";
            case 0x03:
                return "malformed data in request";
            case 0x64:
                return "invalid session ID";
            case 0x65:
                return "invalid data length";
            case 0x69:
                return "unsupported protocol revision";
        }
        return "<unknown>";
    }
}
