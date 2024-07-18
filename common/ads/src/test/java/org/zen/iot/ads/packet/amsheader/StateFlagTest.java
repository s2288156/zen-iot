package org.zen.iot.ads.packet.amsheader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Jack Wu
 */
class StateFlagTest {

    @Test
    void fromBytes_test() {
        assertEquals(StateFlag.REQUEST, StateFlag.fromBytes(StateFlag.REQUEST.toLittleEndianBytes()));
        assertEquals(StateFlag.ADS_COMMAND, StateFlag.fromBytes(StateFlag.ADS_COMMAND.toLittleEndianBytes()));
        assertEquals(StateFlag.RESPONSE, StateFlag.fromBytes(StateFlag.RESPONSE.toLittleEndianBytes()));

        assertNotEquals(StateFlag.REQUEST, StateFlag.fromBytes(StateFlag.RESPONSE.toLittleEndianBytes()));
    }
}
