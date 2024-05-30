package org.zeniot.ads.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Jack Wu
 */
class CommandIdTest {
    @Test
    void toTitleEndianBytes_test() {
        assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x00}, CommandId.READ_DEVICE_INFO.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x02, (byte) 0x00}, CommandId.READ.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x03, (byte) 0x00}, CommandId.WRITE.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x04, (byte) 0x00}, CommandId.READ_STATE.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x05, (byte) 0x00}, CommandId.WRITE_CONTROL.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x06, (byte) 0x00}, CommandId.ADD_DEVICE_NOTIFICATION.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x07, (byte) 0x00}, CommandId.DELETE_DEVICE_NOTIFICATION.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x08, (byte) 0x00}, CommandId.DEVICE_NOTIFICATION.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x09, (byte) 0x00}, CommandId.READ_WRITE.toLittleEndianBytes());
    }
}
