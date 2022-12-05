package org.zeniot.transport.mqtt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeniot.transport.mqtt.service.MqttTransportService;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Component
@Slf4j
public class MqttBrokerServerInitializer {

    @Autowired
    private MqttTransportService mqttTransportService;

    // @PostConstruct
    public void init() throws InterruptedException {
        EventLoopGroup boosGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boosGroup, workerGroup);
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
                    ch.pipeline().addLast("decoder", new MqttDecoder());
                    ch.pipeline().addLast("heartBeatHandler", new IdleStateHandler(45, 0, 0, TimeUnit.SECONDS));
                    ch.pipeline().addLast("handler", new MqttHeartBeatBrokerHandler(mqttTransportService));
                }
            });

            ChannelFuture f = b.bind(1883).sync();
            log.info("MQTT broker initiated... ");

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            boosGroup.shutdownGracefully();
        }
    }
}
