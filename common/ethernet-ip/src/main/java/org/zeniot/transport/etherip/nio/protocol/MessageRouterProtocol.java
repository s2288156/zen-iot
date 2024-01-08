package org.zeniot.transport.etherip.nio.protocol;

import lombok.Getter;
import org.zeniot.transport.etherip.nio.types.CNPath;
import org.zeniot.transport.etherip.nio.types.CNService;
import org.zeniot.transport.etherip.nio.types.CipException;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public class MessageRouterProtocol extends ProtocolAdapter {
    final private CNService service;

    final private CNPath path;

    final protected Protocol body;

    /**
     * -- GETTER --
     *
     * @return Status code of response
     */
    @Getter
    private int status = 0;

    private int[] ext_status = new int[0];

    @Getter
    private boolean partialTransfert = false;
    ;

    /**
     * Initialize
     *
     * @param service Service for request
     * @param path    Path for request
     * @param body    Protocol embedded in the message request/response
     */
    public MessageRouterProtocol(final CNService service, final CNPath path, final Protocol body) {
        this.service = service;
        this.path = path;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRequestSize() {
        return 2 + this.path.getRequestSize() + this.body.getRequestSize();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void encode(final ByteBuffer buf)
            throws Exception {
        buf.put(this.service.getCode());
        this.path.encode(buf);
        this.body.encode(buf);
    }

    @Override
    public int getResponseSize(final ByteBuffer buf) throws Exception {
        throw new IllegalStateException("Unknown response size");
    }

    @Override
    public void decode(final ByteBuffer buf, final int available) throws Exception {
        final byte service_code = buf.get();
        final CNService reply = CNService.forCode(service_code);
        if (reply == null) {
            throw new Exception("Received reply with unknown service code 0x"
                    + Integer.toHexString(service_code));
        }
        if (!reply.isReply()) {
            throw new Exception("Expected reply, got " + reply);
        }

        final int reserved = buf.get();
        this.status = buf.get();
        final int ext_status_size = buf.get();
        this.ext_status = new int[ext_status_size];
        for (int i = 0; i < ext_status_size; ++i) {
            this.ext_status[i] = buf.getShort();
        }


        final CNService expected_reply = this.service.getReply();
        if (this.status != 0) {
            if (this.status == 6) { // Not an error, we need to ask for remaining
                this.partialTransfert = true;
            } else {
                if (ext_status_size > 0) {
                    throw new CipException(this.status, this.ext_status[0]);
                } else {
                    throw new CipException(this.status, 0);
                }
            }
        }

        if (expected_reply != null && expected_reply != reply) {
            throw new Exception(
                    "Expected " + expected_reply + ", got " + reply);
        }

        this.body.decode(buf, available - 4 - 2 * ext_status_size);
    }

    /**
     * @return Extended status codes of response
     */
    public int[] getExtendedStatus() {
        return this.ext_status;
    }
}
