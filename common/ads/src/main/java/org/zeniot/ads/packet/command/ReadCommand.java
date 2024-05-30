package org.zeniot.ads.packet.command;

import org.zeniot.ads.packet.AmsBody;
import org.zeniot.ads.packet.IndexGroup;

/**
 * @author Jack Wu
 */
public class ReadCommand extends AmsBody {

    /**
     * Size: 4 bytes. Index Group of the data which should be read.
     */
    private final IndexGroup indexGroup = IndexGroup.PLC_ADS_IGR_RW_DB;
    /**
     * Size: 4 bytes. Index Offset of the data which should be read.
     */
    private byte[] indexOffset;
    /**
     * Size: 4 bytes. Length of the data (in bytes) which should be read.
     */
    private byte[] length;

    public ReadCommand() {
        indexOffset = new byte[]{};
        length = new byte[]{};
    }

    @Override
    public byte[] commandBytes() {
        byte[] result = new byte[12];
        System.arraycopy(indexGroup.toLittleEndianBytes(), 0, result, 0, 4);
        System.arraycopy(indexOffset, 4, result, 4, 4);
        System.arraycopy(length, 8, result, 8, 4);
        return result;
    }
}
