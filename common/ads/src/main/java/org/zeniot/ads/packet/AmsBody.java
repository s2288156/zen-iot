package org.zeniot.ads.packet;

/**
 * @author Jack Wu
 */
public abstract class AmsBody {
    private AmsTcpHeader header;

    private CommandId commandId;
    /**
     * Request(0) / Response(1): 0x000_
     * ADS command: 0x0004
     */
    private StateFlag stateFlag;
    /**
     * Size: 4 bytes
     * Description: Size of the data range. The unit is byte.
     */
    private Integer length;
    /**
     * Size: 4 bytes
     * Description: AMS error number. See ADS Return Codes.
     * no error - 0x00000000
     */
    private String errorCode;

    /**
     * Size: 4 bytes
     * Description: Free usable 32 bit array. Usually this array serves to send an Id.
     * This Id makes is possible to assign a received response to a request, which was sent before.
     */
    private String invokeId;

    public abstract byte[] commandBytes();
}
