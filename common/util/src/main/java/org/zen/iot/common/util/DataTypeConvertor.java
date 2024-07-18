package org.zen.iot.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jack Wu
 */
@Slf4j
public abstract class DataTypeConvertor {
    /**
     * 不能直接使用Integer.byteValue()，会出现越界问题丢失数据，比如：
     * 851直接byteValue() = 83，而851转十六进制 = 353，再转byte[] = [83, 3]
     *
     * @param hexLength hex长度
     * @return byte[] Little-endian
     */
    public static byte[] toHexToBytes(Integer num, int hexLength) {
        String hex = String.format("%0" + hexLength + "X", num);
        // log.info("num: {}, hex: {}", num, hex);
        return hexToBytes(hex);
    }

    /**
     * @param hex 16进制字符串
     * @return byte[] Little-endian
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[((len - i) / 2) - 1] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static int bytesToIntLittleEndian(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result |= (bytes[i] & 0xFF) << (i * 8);
        }
        return result;
    }

}
