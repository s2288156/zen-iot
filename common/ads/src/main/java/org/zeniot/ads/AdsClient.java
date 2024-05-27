package org.zeniot.ads;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jack Wu
 */
@Slf4j
public class AdsClient {
    private String ip;
    private Integer port;
    private Bootstrap bootstrap;
    private Channel ch;

    public AdsClient(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
        this.bootstrap = new Bootstrap();
        try {
            bootstrap.group(new NioEventLoopGroup(1))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LoggingHandler())
                                    .addLast(new AdsClientHandler());
                        }
                    });
        } catch (Exception e) {
            log.error("", e);
        }
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
