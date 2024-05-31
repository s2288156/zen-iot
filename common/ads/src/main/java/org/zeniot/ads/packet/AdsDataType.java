package org.zeniot.ads.packet;

import org.zeniot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public enum AdsDataType {
    BOOL(1),
    SINT(1),
    USINT(1),
    INT(2),
    UINT(2),
    DINT(4),
    UDINT(4),
    LINT(8),
    ULINT(8),
    REAL(4),
    LREAL(8),
    STRING(256),
    ;
    private int numBytes;

    AdsDataType(int numBytes) {
        this.numBytes = numBytes;
    }

    public byte[] toLittleEndianBytes() {
        return DataTypeConvertor.toHexToBytes(this.numBytes, 8);
    }
}
