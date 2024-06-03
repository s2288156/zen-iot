package org.zeniot.ads.packet.amsheader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jack Wu
 */
class AmsErrorCodeTest {

    @Test
    void fromBytes_test() {
        assertEquals(AmsErrorCode.NO_ERROR, AmsErrorCode.fromBytes(AmsErrorCode.NO_ERROR.toLittleEndianBytes()));
    }
}
