package org.zeniot.ads.packet;

import org.junit.jupiter.api.Test;
import org.zeniot.ads.packet.command.ReadCommand;

/**
 * @author Jack Wu
 */
class AmsBodyTest {

    @Test
    void ReadCommand_test() {
        ReadCommand readCommand = new ReadCommand(385000, AdsDataType.BOOL);

    }
}
