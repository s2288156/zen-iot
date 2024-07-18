package org.zen.iot.ads.packet.amsheader;

import org.apache.commons.lang3.StringUtils;
import org.zen.iot.common.util.DataTypeConvertor;

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


    public static AmsErrorCode fromBytes(byte[] bytes) {
        int result = DataTypeConvertor.bytesToIntLittleEndian(bytes);
        String code = String.format("%08X", result);
        for (AmsErrorCode errorCode : AmsErrorCode.values()) {
            if (StringUtils.equals(errorCode.code, code)) {
                return errorCode;
            }
        }
        return null;
    }
}
