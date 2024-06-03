package org.zeniot.ads.packet;

import lombok.Getter;

/**
 * @author Jack Wu
 */
public class AmsTcpHeader {
    @Getter
    private final byte[] reserved;

    @Getter
    private final byte[] length;

    public AmsTcpHeader() {
        this.reserved = new byte[]{(byte) 0x00, (byte) 0x00};
        this.length = new byte[4];
    }

    public AmsTcpHeader(byte[] length) {
        this.reserved = new byte[]{(byte) 0x00, (byte) 0x00};
        this.length = length;
    }

    public void addLength(Integer length) {
        this.length[0] = (byte) (length & 0xFF);
        this.length[1] = (byte) ((length >> 8) & 0xFF);
        this.length[2] = (byte) ((length >> 16) & 0xFF);
        this.length[3] = (byte) ((length >> 24) & 0xFF);
    }

}
