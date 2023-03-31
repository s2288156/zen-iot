package org.zeniot.transport.mqtt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.data.domain.simulator.transport.SimulatorMqttTransportConfig;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class MqttClient {
    private static final String HOST = System.getProperty("host", "127.0.0.1");
    private static final int PORT = Integer.parseInt(System.getProperty("port", "1883"));
    private final String clientId;
    private final String name;
    private static final String password = System.getProperty("password", "guest");
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private boolean isConnected = false;
    private Channel channel;
    private final MqttClientHandler mqttClientHandler;

    public MqttClient(Simulator simulator) {
        SimulatorMqttTransportConfig mqttTransportConfig = JacksonUtil.convertValue(simulator.getTransportConfig(), SimulatorMqttTransportConfig.class);
        clientId = String.valueOf(simulator.getId());
        name = simulator.getName();
        mqttClientHandler = new MqttClientHandler(clientId, name, password, mqttTransportConfig);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean connect() {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
                ch.pipeline().addLast("decoder", new MqttDecoder());
                ch.pipeline().addLast("heartBeatHandler", new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS));
                ch.pipeline().addLast("handler", mqttClientHandler);
            }
        });

        try {
            channel = b.connect(HOST, PORT).sync().channel();
            log.info("[{}] Client connected", clientId);
            this.isConnected = true;
        } catch (InterruptedException e) {
            log.error("mqtt client start fail");
            shutdown();
        }
        return true;
    }

    public void shutdown() {
        workerGroup.shutdownGracefully();
        this.isConnected = false;
        log.info("shutdown mqtt client, clientId = {}, name = {}", clientId, name);
    }
}
