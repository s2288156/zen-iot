package org.zeniot.ads.packet.command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.zeniot.ads.packet.AdsDataType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jack Wu
 */
@Slf4j
class ReadCommandTest {

    @Test
    void commandBytes_test() {
        ReadCommand readCommand = new ReadCommand(385000, AdsDataType.BOOL);
        byte[] bytes = readCommand.commandBytes();
        log.info("{}", bytes);
        assertEquals(12, bytes.length);
    }
}
