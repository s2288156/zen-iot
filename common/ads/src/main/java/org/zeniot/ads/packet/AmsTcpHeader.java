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

    public static AmsTcpHeader create(Integer length) {
        AmsTcpHeader amsTcpHeader = new AmsTcpHeader();
        amsTcpHeader.length[0] = (byte) (length & 0xFF);
        amsTcpHeader.length[1] = (byte) ((length >> 8) & 0xFF);
        amsTcpHeader.length[2] = (byte) ((length >> 16) & 0xFF);
        amsTcpHeader.length[3] = (byte) ((length >> 24) & 0xFF);
        return amsTcpHeader;
    }

}
