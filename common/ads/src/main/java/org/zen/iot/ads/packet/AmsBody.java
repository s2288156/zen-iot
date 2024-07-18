package org.zen.iot.ads.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.zen.iot.ads.packet.amsheader.AmsErrorCode;
import org.zen.iot.ads.packet.amsheader.CommandId;
import org.zen.iot.ads.packet.amsheader.StateFlag;
import org.zen.iot.ads.util.AdsUtil;
import org.zen.iot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public abstract class AmsBody extends AmsTcpHeader {

    private final CommandId commandId;
    /**
     * Request(0) / Response(1): 0x000_
     * ADS command: 0x0004
     */
    private final StateFlag stateFlag;
    /**
     * Size: 4 bytes
     * Description: Size of the data range. The unit is byte.
     */
    private final Integer length;
    /**
     * Size: 4 bytes
     * Description: AMS error number. See ADS Return Codes.
     * no error - 0x00000000
     */
    private final AmsErrorCode errorCode;

    public abstract byte[] dataBytes();

    public AmsBody(CommandId commandId, StateFlag stateFlag, Integer length) {
        this.commandId = commandId;
        this.stateFlag = stateFlag;
        this.length = length;
        this.errorCode = AmsErrorCode.NO_ERROR;
    }

    public AmsBody(byte[] length, CommandId commandId, StateFlag stateFlag, Integer length1, AmsErrorCode errorCode) {
        super(length);
        this.commandId = commandId;
        this.stateFlag = stateFlag;
        this.length = length1;
        this.errorCode = errorCode;
    }

    public ByteBuf toRequestByteBuf(Integer invokeId, String targetAmsNetId, Integer targetAmsPort, String sourceAmsNetId, Integer sourceAmsPort) {
        byte[] commandIdBytes = commandId.toLittleEndianBytes();
        byte[] stateFlagBytes = stateFlag.toLittleEndianBytes();
        byte[] lengthBytes = DataTypeConvertor.toHexToBytes(this.length, 8);
        byte[] errorCodeBytes = errorCode.toLittleEndianBytes();
        byte[] invokeIdBytes = DataTypeConvertor.toHexToBytes(invokeId, 8);
        byte[] commandBytes = dataBytes();

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

    public static AmsBody fromResponseByteBuf(ByteBuf buf) {
        // AmsBody amsBody = new AmsBody();

        return null;
    }

}
