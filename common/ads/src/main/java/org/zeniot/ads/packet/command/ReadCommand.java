package org.zeniot.ads.packet.command;

import org.zeniot.ads.packet.AdsDataType;
import org.zeniot.ads.packet.AmsBody;
import org.zeniot.ads.packet.amsheader.AmsErrorCode;
import org.zeniot.ads.packet.amsheader.CommandId;
import org.zeniot.ads.packet.amsheader.StateFlag;

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
    private AdsDataType adsDataType;
    // private byte[] readLength;

    public ReadCommand(AdsDataType adsDataType) {
        super(CommandId.READ, StateFlag.ADS_COMMAND, 12, AmsErrorCode.NO_ERROR, "");
        indexOffset = new byte[4];
        // readLength = new byte[4];
        this.adsDataType = adsDataType;
    }

    @Override
    public byte[] commandBytes() {
        byte[] result = new byte[12];
        System.arraycopy(indexGroup.toLittleEndianBytes(), 0, result, 0, 4);
        System.arraycopy(indexOffset, 4, result, 4, 4);
        // System.arraycopy(readLength, 8, result, 8, 4);
        System.arraycopy(adsDataType.toLittleEndianBytes(), 8, result, 8, 4);
        return result;
    }
}
