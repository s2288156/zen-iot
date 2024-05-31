package org.zeniot;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jack Wu
 */
@Slf4j
public class Test1 {
    public static void main(String[] args) {
        String s = "start";
        byte[] bytes = s.getBytes();
        log.info("{}", bytes);
        for (byte aByte : bytes) {
            log.info("{}", Integer.toHexString(aByte));
        }
    }
}
