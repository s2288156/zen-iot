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
        Assertions.assertArrayEquals(new byte[]{(byte) 0xac}, DataTypeConvertor.toHexToBytes(172));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x1c}, DataTypeConvertor.toHexToBytes(28));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x83}, DataTypeConvertor.toHexToBytes(131));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x31}, DataTypeConvertor.toHexToBytes(49));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x01}, DataTypeConvertor.toHexToBytes(1));
        Assertions.assertArrayEquals(new byte[]{(byte) 0x53, (byte) 0x03}, DataTypeConvertor.toHexToBytes(851));
    }

    @Test
    void t1() {
        log.info("1\t:\t{}", Integer.toHexString(1));
        log.info("16\t:\t{}", Integer.toHexString(16));
        log.info("172\t:\t{}", Integer.toHexString(172));
        log.info("851\t:\t{}", Integer.toHexString(851));

        int len = (int) Math.ceil(2 / 2.0);
        log.info("len\t:\t{}", len);
    }
}
