package org.zeniot.ads.packet;

/**
 * @author Jack Wu
 */
public enum AdsCommand {
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

    AdsCommand(int id) {
        this.id = id;
    }
}
