package org.zen.iot.transport.mqtt.broker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zen.iot.transport.api.MqttBrokerService;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Service
class DefaultMqttBrokerService implements MqttBrokerService {

    @Override
    public void receiveMsg(String topic, String payload) {
        log.info("topic = {}, payload = {}", topic, payload);
    }
}
