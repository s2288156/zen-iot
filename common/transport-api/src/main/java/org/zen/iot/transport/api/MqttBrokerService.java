package org.zen.iot.transport.api;

/**
 * @author Wu.Chunyang
 */
public interface MqttBrokerService {
    void receiveMsg(String topic, String payload);
}
