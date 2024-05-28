package org.zeniot;

import lombok.extern.slf4j.Slf4j;
import org.zeniot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
@Slf4j
public class Test1 {
    public static void main(String[] args) {
        log.info("{}", DataTypeConvertor.toHexToBytes(1, 4));
    }
}
