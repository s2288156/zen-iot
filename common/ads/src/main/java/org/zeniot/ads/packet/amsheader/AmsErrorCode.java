package org.zeniot.ads.packet.amsheader;

import org.zeniot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public enum AmsErrorCode {
    /**
     * no error
     */
    NO_ERROR("00000000"),
    ;
    private final String code;

    private static final Integer length = 4;

    AmsErrorCode(String code) {
        this.code = code;
    }

    public byte[] toLittleEndianBytes() {
        return DataTypeConvertor.hexToBytes(this.code);
    }

}
