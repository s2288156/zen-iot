package org.zeniot.ads.packet;

/**
 * @author Jack Wu
 */
public class AmsTcpHeader {
    private byte[] data;

    public AmsTcpHeader() {
        data = new byte[6];
        data[0] = (byte) 0x00;
        data[1] = (byte) 0x00;
    }

    public void putLength(Integer length) {
        data[5] = (byte) ((length >> 24) & 0xFF);
        data[4] = (byte) ((length >> 16) & 0xFF);
        data[3] = (byte) ((length >> 8) & 0xFF);
        data[2] = (byte) (length & 0xFF);
    }
}
