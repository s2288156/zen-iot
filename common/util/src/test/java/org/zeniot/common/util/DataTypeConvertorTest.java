package org.zeniot.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Jack Wu
 */
@Slf4j
class DataTypeConvertorTest {

    @Test
    void toByteArray() {
        // 172.28.131.49.1.1:851 --- 172-256 = -84
        // (byte) 0xac, (byte) 0x1c, (byte) 0x83, (byte) 0x31, (byte) 0x01, (byte) 0x01, (byte) 0x53, (byte) 0x03
        // [-84, 28, -125, 49, 1, 1, 83, 3]
        Assertions.assertArrayEquals(new byte[]{(byte) 0xac}, DataTypeConvertor.toHexToBytes(172, 2));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x1c}, DataTypeConvertor.toHexToBytes(28, 2));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x83}, DataTypeConvertor.toHexToBytes(131, 2));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x31}, DataTypeConvertor.toHexToBytes(49, 2));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x01}, DataTypeConvertor.toHexToBytes(1, 2));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x53, (byte) 0x03}, DataTypeConvertor.toHexToBytes(851, 4));
    }

    @Test
    void hexToBytes_test() {
        String groupIndex = "00004040";
        byte[] bytes = DataTypeConvertor.hexToBytes(groupIndex);
        Assertions.assertArrayEquals(new byte[]{(byte) 0x40, (byte) 0x40, (byte) 0x00, (byte) 0x00}, bytes);
    }
}
