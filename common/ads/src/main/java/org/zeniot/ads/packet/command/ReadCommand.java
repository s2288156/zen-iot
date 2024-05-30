package org.zeniot.ads.packet.command;

/**
 * @author Jack Wu
 */
public class ReadCommand {

    /**
     * Size: 4 bytes. Index Group of the data which should be read.
     */
    private byte[] indexGroup;
    /**
     * Size: 4 bytes. Index Offset of the data which should be read.
     */
    private byte[] indexOffset;
    /**
     * Size: 4 bytes. Length of the data (in bytes) which should be read.
     */
    private byte[] length;
}
