package org.zeniot.transport.etherip.nio;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public interface ProtocolDecoder {
    /**
     * Determine the expected response size.
     * <p>
     * Some protocol elements have a known, fixed response size. In that case this method returns that number.
     * <p>
     * In other cases the exact response size can depend on a header in the response. This method should then first return the size of the expected header. On subsequent calls is can peek into the
     * buffer to extract the size of the complete response from for example a 'count' in the header.
     * <p>
     * Buffer is positioned according to what has been read to far, which may be nothing.
     *
     * @param buf {@link ByteBuffer} from which to decode protocol
     * @return Number of remaining bytes needed. 0 if buffer contains complete protocol response.
     */
    int getResponseSize(ByteBuffer buf) throws Exception;

    /**
     * Decode protocol from buffer.
     * <p>
     * Buffer is initially positioned at its start. This method should then read data from the buffer, leaving it positioned at the end of the data handled by this protocol segment.
     * <p>
     * When using this method in a protocol class hierarchy, the 'base' class can decode the header of a protocol, and derived classes can then decode the remaining bytes.
     *
     * @param buf       {@link ByteBuffer} from which to decode protocol
     * @param available Number of bytes available for this protocol section (buffer may contain more follow-on bytes for surrounding protocol wrappers)
     */
    void decode(ByteBuffer buf, int available) throws Exception;
}
