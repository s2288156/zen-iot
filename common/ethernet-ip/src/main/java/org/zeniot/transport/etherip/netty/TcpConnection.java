package org.zeniot.transport.etherip.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jack Wu
 */
@Slf4j
public class TcpConnection {

    private String ip;
    private int port = 44818;
    private final Bootstrap bootstrap = new Bootstrap();
    private Channel ch;
    private EventLoopGroup workerGroup;

    public void connect(String ip) {
        this.ip = ip;
        workerGroup = new NioEventLoopGroup(1);
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new EtherNetTcpInboundHandler());
                    }
                });

        try {
            this.ch = bootstrap.connect(ip, port).sync().channel();
            log.info("ip: {}, port: {}, connect success!", ip, port);
        } catch (InterruptedException e) {
            log.error("tcp connect error:", e);
            this.ch.close();
        }
    }

    public void close() {
        log.info("closing connect: {}:{}", ip, port);
        try {
            this.ch.close().get();
        } catch (Exception e) {
            log.error("close error:", e);
        } finally {
            workerGroup.shutdownGracefully();
        }
        log.info("closed connect {}:{}", ip, port);
    }

    public void write(ByteBuf req) {
        ch.writeAndFlush(req);
        log.info(">>>>>>>>>>>>>>>>>");
    }

}
