package org.zeniot.ads;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jack Wu
 */
@Slf4j
public class TcpServer {
    NioEventLoopGroup boosGroup;
    NioEventLoopGroup workerGroup;
    ServerBootstrap b;
    Channel channel;

    @SneakyThrows
    public void start() {
        boosGroup = new NioEventLoopGroup(2);
        workerGroup = new NioEventLoopGroup(2);
        b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(boosGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LoggingHandler(LogLevel.INFO))
                                .addLast(new AdsClientHandler());
                    }
                });
        if (channel == null) {
            channel = b.bind(48898).sync().channel();
            channel.closeFuture().sync();
        }
        if (channel != null && channel.isActive()) {
            log.info("tpc模拟器已启动.....");
        }
    }

    public void destroy() {
        boosGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public static void main(String[] args) {
        TcpServer tcpServer = new TcpServer();
        tcpServer.start();
    }
}
