package org.zeniot.transport.etherip.nio.protocol;

import org.zeniot.transport.etherip.nio.Connection;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jack Wu
 */
public class CIPData {
    public enum Type {
        BOOL(0x00C1, 1),
        SINT(0x00C2, 1),
        INT(0x00C3, 2),
        DINT(0x00C4, 4),
        REAL(0x00CA, 4),
        BITS(0x00D3, 4),
        // Order of enums matter: BITS is the last numeric type (not-string)
        STRUCT(0x02A0, 0),

        /**
         * Experimental: The ENET doc just shows several structures for TIMER, COUNTER, CONTROL and indicates that the T_CIP_STRUCT = 0x02A0 is followed by two more bytes, shown as "?? ??". Looks like
         * for strings, those are always 0x0FCE, followed by DINT length, characters and more zeroes
         */
        STRUCT_STRING(0x0FCE, 0);

        final private short code;
        final private int element_size;

        final private static Map<Short, Type> reverse;

        static {
            reverse = new HashMap<>();
            for (final Type t : EnumSet.allOf(Type.class)) {
                reverse.put(t.code, t);
            }
        }

        public static Type forCode(final short code) throws Exception {
            return reverse.get(code);
        }

        Type(final int code, final int element_size) {
            this.code = (short) code;
            this.element_size = element_size;
        }

        @Override
        final public String toString() {
            return this.name() + String.format(" (0x%04X)", this.code);
        }
    }

    /**
     * Data type
     */
    final private Type type;

    /**
     * Number of elements (i.e. number of array elements, not bytes)
     */
    final private short elements;

    /**
     * Raw data, not including type code or element count
     */
    final private ByteBuffer data;

    /**
     * Initialize
     *
     * @param type Data type
     * @param data Bytes that contain the raw CIP data
     * @throws Exception when data is invalid
     */
    public CIPData(final Type type, final byte[] data) throws Exception {
        this.type = type;
        this.data = ByteBuffer.allocate(data.length);
        this.data.order(Connection.BYTE_ORDER);
        this.data.put(data);
        this.elements = this.determineElementCount();
    }

    /**
     * @return Number of elements
     */
    private short determineElementCount() throws Exception {
        switch (this.type) {
            case BOOL:
            case SINT:
            case INT:
            case DINT:
            case BITS:
            case REAL:
                return (short) (this.data.capacity() / this.type.element_size);
            case STRUCT: {
                return 1; // We do not know without byte packing details but at least 1.
            }
            default:
                throw new Exception("Type " + this.type + " not handled");
        }
    }

    /**
     * @return String representation for debugging
     */
    @Override
    final synchronized public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("CIP_").append(this.type).append(": ");
        final ByteBuffer buf = this.data.asReadOnlyBuffer();
        buf.order(this.data.order());
        buf.clear();
        switch (this.type) {
            case BOOL:
            case SINT: {
                final byte[] values = new byte[this.elements];
                buf.get(values);
                result.append(Arrays.toString(values));
                break;
            }
            case INT: {
                final short[] values = new short[this.elements];
                for (int i = 0; i < this.elements; ++i) {
                    values[i] = buf.getShort();
                }
                result.append(Arrays.toString(values));
                break;
            }
            case DINT:
            case BITS: {
                final int[] values = new int[this.elements];
                for (int i = 0; i < this.elements; ++i) {
                    values[i] = buf.getInt();
                }
                result.append(Arrays.toString(values));
                break;
            }
            case REAL: {
                final float[] values = new float[this.elements];
                for (int i = 0; i < this.elements; ++i) {
                    values[i] = buf.getFloat();
                }
                result.append(Arrays.toString(values));
                break;
            }
            case STRUCT: {
                final short code = buf.getShort();
                final Type el_type;
                try {
                    el_type = Type.forCode(code);
                } catch (final Exception ex) {
                    result.append("Structure element with type code 0x"
                            + Integer.toHexString(code));
                    break;
                }
                if (el_type == Type.STRUCT_STRING) {
                    result.append(Type.STRUCT_STRING).append(" ");
                    final int len = buf.getInt();
                    final byte[] chars = new byte[len];
                    buf.get(chars);
                    final String value = new String(chars);
                    result.append("'").append(value).append("', len " + len);
                } else {
                    result.append("Structure element of type " + this.type);
                }
                break;
            }
            default:
                result.append("Unknown Type " + this.type);
        }
        return result.toString();
    }
}
