package org.zeniot.transport.api;

/**
 * @author Wu.Chunyang
 */
public interface MqttTransportService {
    void receiveMsg(String topic, String payload);
}
