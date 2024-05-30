package org.zeniot.common.util;

import java.util.regex.Pattern;

/**
 * @author Jack Wu
 */
public abstract class PatternValidator {

    public static boolean isAmsNetId(String amsNetId) {
        String AMS_NET_ID_PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        return Pattern.compile(AMS_NET_ID_PATTERN).matcher(amsNetId).matches();
    }
}
