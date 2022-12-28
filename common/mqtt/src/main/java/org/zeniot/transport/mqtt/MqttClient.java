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
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.data.domain.transport.MqttTransportConfig;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class MqttClient {
    private static final String HOST = System.getProperty("host", "127.0.0.1");
    private static final int PORT = Integer.parseInt(System.getProperty("port", "1883"));
    private final String CLIENT_ID;
    private final String USER_NAME;
    private static final String PASSWORD = System.getProperty("password", "guest");
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private boolean isConnected = false;

    private MqttClient(String clientId, String userName) {
        CLIENT_ID = clientId;
        USER_NAME = userName;
    }

    public MqttClient(Simulator simulator) {
        MqttTransportConfig mqttTransportConfig = JacksonUtil.convertValue(simulator.getTransportConfig(), MqttTransportConfig.class);
        CLIENT_ID = String.valueOf(simulator.getId());
        USER_NAME = simulator.getName();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean connect() {
        Thread thread = new Thread(() -> {
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

            try {
                ChannelFuture f = b.connect(HOST, PORT).sync();
                log.info("[{}] Client connected", CLIENT_ID);
                this.isConnected = true;
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("mqtt client start fail");
                shutdown();
            }

        });
        thread.start();
        if (confirmConnect()) {
            return true;
        } else {
            return false;
        }

    }

    private boolean confirmConnect() {
        int i = 0, max = 50;
        while (!isConnected && i < max) {
            i++;
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        if (i == max) {
            return false;
        }
        return true;
    }


    public void shutdown() {
        workerGroup.shutdownGracefully();
        this.isConnected = false;
        log.info("shutdown mqtt client, clientId = {}, username = {}", CLIENT_ID, USER_NAME);
    }
}
