package org.zeniot.ads.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Jack Wu
 */
class AdsUtilTest {
    @Test
    void parseAmsNetId_test() {
        String amsNetId = "172.28.131.49.1.1";
        byte[] expectedAdsNetIdBytes = new byte[]{(byte) 0xac, (byte) 0x1c, (byte) 0x83, (byte) 0x31, (byte) 0x01, (byte) 0x01};
        assertArrayEquals(expectedAdsNetIdBytes, AdsUtil.parseAmsNetId(amsNetId));
    }

    @Test
    void parseAmsNetId_errorFormat_test() {
        assertThrows(IllegalArgumentException.class, () -> AdsUtil.parseAmsNetId("127.0.0.1"));
        assertThrows(IllegalArgumentException.class, () -> AdsUtil.parseAmsNetId("127.0.0.1.2."));
        assertThrows(IllegalArgumentException.class, () -> AdsUtil.parseAmsNetId("127.0..1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> AdsUtil.parseAmsNetId("127.0.0.1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> AdsUtil.parseAmsNetId("127.0.0.1.1.1."));
    }
}
