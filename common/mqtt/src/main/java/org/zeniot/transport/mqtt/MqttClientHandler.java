package org.zeniot.transport.mqtt;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class MqttClientHandler extends ChannelInboundHandlerAdapter {

    private final String clientId;
    private final String userName;
    private final byte[] password;

    public MqttClientHandler(String clientId, String userName, String password) {
        this.clientId = clientId;
        this.userName = userName;
        this.password = password.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // discard all message
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        MqttFixedHeader connectFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnectVariableHeader connectVariableHeader = new MqttConnectVariableHeader(MqttVersion.MQTT_3_1_1.protocolName(), MqttVersion.MQTT_3_1_1.protocolLevel(), true, true, false, 0, false, false, 20, MqttProperties.NO_PROPERTIES);
        MqttConnectPayload connectPayload = new MqttConnectPayload(clientId, MqttProperties.NO_PROPERTIES, null, null, userName, password);
        MqttConnectMessage connectMessage = new MqttConnectMessage(connectFixedHeader, connectVariableHeader, connectPayload);
        ctx.writeAndFlush(connectMessage);
        log.info("[{}] Sent CONNECT", clientId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
