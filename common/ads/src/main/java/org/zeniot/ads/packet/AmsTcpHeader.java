package org.zeniot.ads.packet;

/**
 * @author Jack Wu
 */
public class AmsTcpHeader {
    private byte[] reserved;

    private byte[] length;

    public AmsTcpHeader() {
        reserved = new byte[]{(byte) 0x00, (byte) 0x00};
        length = new byte[4];
    }

    public void putLength(Integer length) {
        // data[5] = (byte) ((length >> 24) & 0xFF);
        // data[4] = (byte) ((length >> 16) & 0xFF);
        // data[3] = (byte) ((length >> 8) & 0xFF);
        // data[2] = (byte) (length & 0xFF);
    }
}
