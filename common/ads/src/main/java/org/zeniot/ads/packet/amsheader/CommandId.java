package org.zeniot.ads.packet.amsheader;

import org.zeniot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public enum CommandId {
    INVALID(0),
    READ_DEVICE_INFO(1),
    READ(2),
    WRITE(3),
    READ_STATE(4),
    WRITE_CONTROL(5),
    ADD_DEVICE_NOTIFICATION(6),
    DELETE_DEVICE_NOTIFICATION(7),
    DEVICE_NOTIFICATION(8),
    READ_WRITE(9),
    ;
    private final int id;

    public static final Integer BYTE_SIZE = 2;

    CommandId(int id) {
        this.id = id;
    }

    public byte[] toLittleEndianBytes() {
        return DataTypeConvertor.toHexToBytes(this.id, BYTE_SIZE * 2);
    }

    public static CommandId fromBytes(byte[] bytes) {
        int commandId = DataTypeConvertor.bytesToIntLittleEndian(bytes);
        return CommandId.values()[commandId];
    }

}
