package org.zeniot.transport.mqtt.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.data.domain.simulator.transport.SimulatorMqttTransportConfig;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@ChannelHandler.Sharable
public class MqttClientHandler extends ChannelInboundHandlerAdapter {

    private final String clientId;
    private final String userName;
    private final byte[] password;

    private final ScheduledExecutorService executorService;
    private final AtomicInteger packetId = new AtomicInteger();
    private final SimulatorMqttTransportConfig transportConfig;

    public MqttClientHandler(String clientId, String userName, String password, SimulatorMqttTransportConfig mqttTransportConfig) {
        this.clientId = clientId;
        this.userName = userName;
        this.password = password.getBytes(StandardCharsets.UTF_8);
        executorService = Executors.newSingleThreadScheduledExecutor();
        transportConfig = mqttTransportConfig;
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

        executorService.scheduleAtFixedRate(() -> {

            MqttFixedHeader publishFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttPublishVariableHeader publishVariableHeader = new MqttPublishVariableHeader(transportConfig.getSaveTimeseriesTopic(), packetId.incrementAndGet());

            ObjectNode payloadJsonNode = JacksonUtil.newObjectNode();
            transportConfig.getTimeseriesFields()
                    .forEach(fieldDefinition -> payloadJsonNode.put(fieldDefinition.getName(), fieldDefinition.nextRandomValue()));
            MqttPublishMessage publishMessage = new MqttPublishMessage(
                    publishFixedHeader,
                    publishVariableHeader,
                    Unpooled.wrappedBuffer(JacksonUtil.toJsonStr(payloadJsonNode).getBytes(StandardCharsets.UTF_8))
            );
            ctx.writeAndFlush(publishMessage);
        }, 0, transportConfig.getPeriod(), transportConfig.getTimeUnit());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        log.error("simulator mqtt client [{}] error: ", clientId, cause);
        ctx.close();
    }

    public void destroy() {
        this.executorService.shutdown();
    }
}
