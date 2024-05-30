package org.zeniot.ads.packet;

/**
 * @author Jack Wu
 */
public class AmsHeader<T> {
    // private String targetAmsNetId;
    // private Integer targetAmsPort;
    // private String sourceAmsNetId;
    // private Integer sourceAmsPort;
    private AdsCommandId commandId;
    /**
     * Request(0) / Response(1): 0x000_
     * ADS command: 0x0004
     */
    private byte[] stateFlags;
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

    /**
     * Size: n bytes
     * Description: Data range. The data range contains the parameter of the considering ADS commands.
     */
    private T adsCommand;


}
