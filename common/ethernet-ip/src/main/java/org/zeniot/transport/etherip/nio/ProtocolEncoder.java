package org.zeniot.transport.etherip.nio;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public interface ProtocolEncoder {
    /**
     * @return Size of the request in bytes
     */
    int getRequestSize();

    /**
     * Encode protocol into the buffer.
     * <p>
     * Buffer must be positioned where the encoding should start. This method then advances the buffer position as it adds bytes.
     * <p>
     * Several <code>encode</code> calls can be used to append more and more content to the buffer. Finally, the buffer typically needs to be 'flipped' for writing the accumulated content.
     *
     * @param buf {@link ByteBuffer} where protocol should be encoded
     */
    void encode(ByteBuffer buf) throws Exception;
}
