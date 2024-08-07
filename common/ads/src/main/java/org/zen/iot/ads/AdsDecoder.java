package org.zen.iot.ads;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.zen.iot.ads.packet.amsheader.CommandId;
import org.zen.iot.ads.packet.amsheader.StateFlag;

import java.util.List;

/**
 * @author Jack Wu
 */
@Slf4j
public class AdsDecoder extends ByteToMessageDecoder {

    private final int headerLength = 6;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        if (discardIllegalByte(in)) return;

        log.info("{}", ByteBufUtil.getBytes(in));
        int readableBytes = in.readableBytes();
        log.info("readable {} bytes", readableBytes);
        if (readableBytes <= headerLength) {
            return;
        }
        // reserved: 2 bytes
        in.readBytes(2);
        int bodyLength = in.readIntLE();
        if (readableBytes < headerLength + bodyLength) {
            in.resetReaderIndex();
            return;
        }
        ByteBuf targetNetIdPort = in.readBytes(8);
        ByteBuf sourceNetIdPort = in.readBytes(8);
        ByteBuf commandId = in.readBytes(CommandId.BYTE_SIZE);
        ByteBuf stateFlag = in.readBytes(StateFlag.BYTE_SIZE);
        int responseLength = in.readIntLE(); // 4 bytes
        int errorCode = in.readIntLE();
        ByteBuf invokeId = in.readBytes(4);

        if (errorCode > 0) {
            log.error("readerIndex: {}, error code {}", in.readerIndex(), errorCode);
            return;
        }

        // response data
        // ADS error number
        ByteBuf result = in.readBytes(4);
        // Length of data which are supplied back.
        int dataLength = in.readIntLE();
        // Data which are supplied back.
        ByteBuf data = in.readBytes(responseLength - 4 - 4);

        log.info("readerIndex: {}, errorCode: {}, commandId: {}, stateFlag: {} ,responseLength: {}, invokeId: {}, result: {}， dataLength: {}, data: {}",
                in.readerIndex(),
                errorCode,
                ByteBufUtil.getBytes(commandId),
                ByteBufUtil.getBytes(stateFlag),
                responseLength,
                ByteBufUtil.getBytes(invokeId),
                ByteBufUtil.getBytes(result),
                dataLength,
                ByteBufUtil.getBytes(data));

    }

    private static boolean discardIllegalByte(ByteBuf in) {
        byte firstByte = in.readByte();
        if (firstByte != 0) {
            log.info("discard value: {}", firstByte);
            return true;
        }
        in.resetReaderIndex();
        return false;
    }
}
