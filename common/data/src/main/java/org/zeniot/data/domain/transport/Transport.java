package org.zeniot.data.domain.transport;

/**
 * @author Wu.Chunyang
 */
public interface Transport {
    String getHost();

    void setHost(String host);

    int getPort();

    int setPort(int port);
}
