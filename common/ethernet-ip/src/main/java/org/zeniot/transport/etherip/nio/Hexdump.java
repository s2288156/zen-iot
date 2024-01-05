package org.zeniot.transport.etherip.nio;

import java.nio.ByteBuffer;

/**
 * @author Jack Wu
 */
public class Hexdump {
    private static final int ELEMENTS_PER_LINE = 16;

    public static String toAscii(final byte... bytes) {
        return toAscii(ByteBuffer.wrap(bytes)); // byte order does not matter, only byte-wise access
    }

    public static String toAscii(final ByteBuffer buffer) {
        final StringBuilder out = new StringBuilder();
        ascii(buffer, out);
        return out.toString();
    }

    public static void ascii(final ByteBuffer buffer, final StringBuilder out) {
        final int start = buffer.position();
        final int end = buffer.limit();
        for (int pos = 0; pos < end; pos += ELEMENTS_PER_LINE) {
            out.append(String.format("%04X - ", pos));
            for (int i = 0; i < ELEMENTS_PER_LINE; ++i) {
                final int idx = start + pos + i;
                if (idx >= end) {
                    out.append(" ");
                } else {
                    out.append(escapeChars(buffer.get(idx)));
                }
            }
            out.append("\n");
        }
        buffer.rewind();
    }

    /**
     * @param bytes Any bytes
     * @return Printable string where any non-ASCII chars are replaced by '.'
     */
    public static String escapeChars(final byte... bytes) {
        final StringBuilder buf = new StringBuilder();
        for (final byte b : bytes) {
            if (b >= 32 && b < 127) {
                buf.append(Character.toChars(b));
            } else {
                buf.append('.');
            }
        }
        return buf.toString();
    }

}
