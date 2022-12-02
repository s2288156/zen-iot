package org.zeniot.transport.mqtt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Service
class DefaultMqttTransportService implements MqttTransportService {

    @Override
    public void receiveMsg(String topic, String payload) {
        log.info("topic = {}, payload = {}", topic, payload);
    }
}
