package org.zeniot.common.util;

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
     * @return byte[] 小端序
     */
    public static byte[] toHexToBytes(Integer num, int desiredLength) {
        // int length = calculatePaddingLength(num);
        String hex = String.format("%0" + desiredLength + "X", num);
        log.info("num: {}, hex: {}", num, hex);
        return hexToBytes(hex);
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[((len - i) / 2) - 1] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static int calculatePaddingLength(int number) {
        String hex = Integer.toHexString(number);
        return hex.length() + hex.length() % 2;
    }
}
