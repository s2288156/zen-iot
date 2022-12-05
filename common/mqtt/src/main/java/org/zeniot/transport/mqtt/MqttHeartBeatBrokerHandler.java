package org.zeniot.transport.mqtt;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeniot.transport.api.MqttTransportService;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@ChannelHandler.Sharable
public class MqttHeartBeatBrokerHandler extends ChannelInboundHandlerAdapter {

    private MqttTransportService mqttTransportService;

    private MqttHeartBeatBrokerHandler() {
    }

    public MqttHeartBeatBrokerHandler(MqttTransportService mqttTransportService) {
        this.mqttTransportService = mqttTransportService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        MqttMessage mqttMessage = (MqttMessage) msg;
        log.debug("Receive MQTT message: {}", mqttMessage);
        switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT -> {
                MqttConnectMessage connectMsg = (MqttConnectMessage) msg;
                if (StringUtils.isNotBlank(connectMsg.payload().userName())) {
                    log.info("username: {}, password: {}", connectMsg.payload().userName(), StringUtils.toEncodedString(connectMsg.payload().passwordInBytes(), StandardCharsets.UTF_8));
                }
                MqttFixedHeader connackFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, AT_MOST_ONCE, false, 0);
                MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
                MqttConnAckMessage connack = new MqttConnAckMessage(connackFixedHeader, mqttConnAckVariableHeader);
                ctx.writeAndFlush(connack);
            }
            case PUBLISH -> {
                MqttPublishMessage publishMsg = (MqttPublishMessage) msg;

                mqttTransportService.receiveMsg(publishMsg.variableHeader().topicName(), publishMsg.payload().toString(StandardCharsets.UTF_8));

                if (publishMsg.variableHeader().packetId() > 0) {
                    MqttFixedHeader pubAckFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
                    MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(publishMsg.variableHeader().packetId());
                    MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(pubAckFixedHeader, idVariableHeader);
                    ctx.writeAndFlush(pubAckMessage);
                }
            }
            case SUBSCRIBE -> {
                MqttSubscribeMessage subscribeMsg = (MqttSubscribeMessage) msg;
                MqttFixedHeader subAckFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_MOST_ONCE, false, 0);
                // MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(subAckFixedHeader,);
            }
            case PINGREQ -> {
                MqttFixedHeader pingReqFixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, AT_MOST_ONCE, false, 0);
                MqttMessage pingResp = new MqttMessage(pingReqFixedHeader);
                ctx.writeAndFlush(pingResp);
            }
            case DISCONNECT -> ctx.close();
            default -> {
                log.info("Unexpected message type: {}", mqttMessage.fixedHeader().messageType());
                ReferenceCountUtil.release(msg);
                ctx.close();
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.info("Channel heartBeat lost");
        if (evt instanceof IdleStateEvent && IdleState.READER_IDLE == ((IdleStateEvent) evt).state()) {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
