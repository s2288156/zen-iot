package org.zen.iot.ads;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.zen.iot.ads.packet.AmsBody;

/**
 * @author Jack Wu
 */
@Slf4j
public class AdsClient {
    private final String ip;
    private final Integer port;
    private final String targetAmsNetId;
    private final Integer targetAmsPort;
    private final String sourceAmsNetId;
    private final Integer sourceAmsPort;
    private final Bootstrap bootstrap;
    private Channel ch;

    /**
     * Size: 4 bytes
     * Description: Free usable 32 bit array. Usually this array serves to send an Id.
     * This Id makes is possible to assign a received response to a request, which was sent before.
     */
    private static Integer invokeId = 0;

    public AdsClient(String ip, Integer port, String targetAmsNetId, Integer targetAmsPort, String sourceAmsNetId, Integer sourceAmsPort) {
        this.ip = ip;
        this.port = port;
        this.targetAmsNetId = targetAmsNetId;
        this.targetAmsPort = targetAmsPort;
        this.sourceAmsNetId = sourceAmsNetId;
        this.sourceAmsPort = sourceAmsPort;
        this.bootstrap = new Bootstrap();
        try {
            bootstrap.group(new NioEventLoopGroup(1))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast(new AdsDecoder())
                                    .addLast(new AdsClientHandler());
                        }
                    });
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public synchronized void write(AmsBody amsBody) {
        ByteBuf byteBuf = amsBody.toRequestByteBuf(invokeId++, targetAmsNetId, targetAmsPort, sourceAmsNetId, sourceAmsPort);
        log.info("{}", ByteBufUtil.getBytes(byteBuf));
        ch.writeAndFlush(byteBuf);
    }

    public synchronized void writeMsg(byte[] data) {
        try {
            log.info("isOpen: {}, writeMsg >> {}", ch.isOpen(), data);
            ByteBuf requestBody = ByteBufAllocator.DEFAULT.buffer().writeBytes(data);
            ch.writeAndFlush(requestBody).sync();
        } catch (Exception e) {
            log.error("client write exception: ", e);
        }
    }

    public void connect() {
        try {
            ch = bootstrap.connect(ip, port).sync().channel();
        } catch (Exception e) {
            log.error("client connect error: ", e);
        }
    }

    public void close() {
        log.info("client close {}", ch.close().isSuccess());
    }
}
