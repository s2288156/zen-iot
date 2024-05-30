package org.zeniot.ads.packet;

import org.zeniot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public enum StateFlag {
    REQUEST(0),
    RESPONSE(1),
    ADS_COMMAND(4),
    ;
    private final int id;

    private static final Integer length = 2;

    StateFlag(int id) {
        this.id = id;
    }

    public byte[] toLittleEndianBytes() {
        return DataTypeConvertor.toHexToBytes(this.id, length * 2);
    }

}
