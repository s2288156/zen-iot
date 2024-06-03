package org.zeniot.ads.packet.command;

import org.zeniot.ads.packet.AdsDataType;
import org.zeniot.ads.packet.AmsBody;
import org.zeniot.ads.packet.amsheader.AmsErrorCode;
import org.zeniot.ads.packet.amsheader.CommandId;
import org.zeniot.ads.packet.amsheader.StateFlag;
import org.zeniot.common.util.DataTypeConvertor;

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
    private Integer indexOffset;
    /**
     * Size: 4 bytes. Length of the data (in bytes) which should be read.
     */
    private AdsDataType adsDataType;

    // private byte[] readLength;

    public ReadCommand(Integer indexOffset, AdsDataType adsDataType) {
        super(CommandId.READ, StateFlag.ADS_COMMAND, 12, AmsErrorCode.NO_ERROR);
        this.indexOffset = indexOffset;
        this.adsDataType = adsDataType;
    }

    @Override
    public byte[] commandBytes() {
        byte[] result = new byte[12];
        System.arraycopy(indexGroup.toLittleEndianBytes(), 0, result, 0, 4);
        System.arraycopy(DataTypeConvertor.toHexToBytes(indexOffset, 8), 0, result, 4, 4);
        System.arraycopy(adsDataType.toLittleEndianBytes(), 0, result, 8, 4);
        return result;
    }
}
