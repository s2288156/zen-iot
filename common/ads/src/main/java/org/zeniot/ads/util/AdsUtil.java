package org.zeniot.ads.util;

import org.zeniot.common.util.PatternValidator;

/**
 * @author Jack Wu
 */
public abstract class AdsUtil {
    public static byte[] parseAmsNetId(String amsNetId) {
        byte[] result = new byte[6];
        String[] octets = amsNetId.split("\\.");
        if (!PatternValidator.isAmsNetId(amsNetId)) {
            throw new IllegalArgumentException("Invalid amsNetId: " + amsNetId + " | Correct format: 127.0.0.1.1.1");
        }
        for (int i = 0; i < octets.length; i++) {
            result[i] = Integer.valueOf(octets[i]).byteValue();
        }
        return result;
    }
}
