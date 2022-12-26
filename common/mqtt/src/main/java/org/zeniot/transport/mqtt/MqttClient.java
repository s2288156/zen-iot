package org.zeniot.transport.mqtt;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class MqttClient {
    private MqttClient() {
    }

    private static final String HOST = System.getProperty("host", "127.0.0.1");
    private static final int PORT = Integer.parseInt(System.getProperty("port", "1883"));
    private static final String CLIENT_ID = System.getProperty("clientId", "guestClient");
    private static final String USER_NAME = System.getProperty("userName", "guest");
    private static final String PASSWORD = System.getProperty("password", "guest");

    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void init() throws Exception {

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
                ch.pipeline().addLast("decoder", new MqttDecoder());
                ch.pipeline().addLast("heartBeatHandler", new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS));
                ch.pipeline().addLast("handler", new MqttClientHandler(CLIENT_ID, USER_NAME, PASSWORD));
            }
        });

        ChannelFuture f = b.connect(HOST, PORT).sync();
        log.info("[{}] Client connected", CLIENT_ID);
        f.channel().closeFuture().sync();

    }

    public void shutdown() {
        workerGroup.shutdownGracefully();
    }
}
