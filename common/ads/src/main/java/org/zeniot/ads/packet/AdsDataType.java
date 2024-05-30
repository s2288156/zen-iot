package org.zeniot.ads.packet;

/**
 * @author Jack Wu
 */
public enum AdsDataType {
    BOOL(1),
    SINT(2),
    ;
    private int length;

    AdsDataType(int length) {
        this.length = length;
    }
}
