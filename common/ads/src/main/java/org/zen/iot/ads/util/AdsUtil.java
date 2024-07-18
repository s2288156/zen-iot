package org.zen.iot.ads.util;

import org.zen.iot.common.util.DataTypeConvertor;
import org.zen.iot.common.util.PatternValidator;

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

    public static byte[] parseAmsPort(Integer amsPort) {
        byte[] result = DataTypeConvertor.toHexToBytes(amsPort, 4);
        if (result.length != 2) {
            throw new IllegalArgumentException("Invalid amsPort: " + amsPort);
        }
        return result;
    }
}
