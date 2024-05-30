package org.zeniot.ads.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Jack Wu
 */
class AdsCommandIdTest {
    @Test
    void toTitleEndianBytes_test() {
        assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x00}, AdsCommandId.READ_DEVICE_INFO.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x02, (byte) 0x00}, AdsCommandId.READ.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x03, (byte) 0x00}, AdsCommandId.WRITE.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x04, (byte) 0x00}, AdsCommandId.READ_STATE.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x05, (byte) 0x00}, AdsCommandId.WRITE_CONTROL.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x06, (byte) 0x00}, AdsCommandId.ADD_DEVICE_NOTIFICATION.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x07, (byte) 0x00}, AdsCommandId.DELETE_DEVICE_NOTIFICATION.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x08, (byte) 0x00}, AdsCommandId.DEVICE_NOTIFICATION.toLittleEndianBytes());
        assertArrayEquals(new byte[]{(byte) 0x09, (byte) 0x00}, AdsCommandId.READ_WRITE.toLittleEndianBytes());
    }
}
