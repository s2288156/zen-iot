package org.zeniot.ads.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.zeniot.ads.packet.amsheader.AmsErrorCode;
import org.zeniot.ads.packet.amsheader.CommandId;
import org.zeniot.ads.packet.amsheader.StateFlag;
import org.zeniot.ads.util.AdsUtil;
import org.zeniot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public abstract class AmsBody extends AmsTcpHeader {

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
    private AmsErrorCode errorCode;

    public abstract byte[] commandBytes();

    public AmsBody(CommandId commandId, StateFlag stateFlag, Integer length, AmsErrorCode errorCode) {
        this.commandId = commandId;
        this.stateFlag = stateFlag;
        this.length = length;
        this.errorCode = errorCode;
    }

    public ByteBuf toByteBuf(Integer invokeId, String targetAmsNetId, Integer targetAmsPort, String sourceAmsNetId, Integer sourceAmsPort) {
        byte[] commandBytes = commandBytes();

        byte[] commandIdBytes = commandId.toLittleEndianBytes();
        byte[] stateFlagBytes = stateFlag.toLittleEndianBytes();
        byte[] lengthBytes = DataTypeConvertor.toHexToBytes(commandBytes.length, 8);
        byte[] errorCodeBytes = errorCode.toLittleEndianBytes();
        byte[] invokeIdBytes = DataTypeConvertor.toHexToBytes(invokeId, 8);

        super.addLength(32 + commandBytes.length);

        return Unpooled.wrappedBuffer(
                super.getReserved(),
                super.getLength(),
                AdsUtil.parseAmsNetId(targetAmsNetId),
                AdsUtil.parseAmsPort(targetAmsPort),
                AdsUtil.parseAmsNetId(sourceAmsNetId),
                AdsUtil.parseAmsPort(sourceAmsPort),
                commandIdBytes,
                stateFlagBytes,
                lengthBytes,
                errorCodeBytes,
                invokeIdBytes,
                commandBytes
        );
    }

}
