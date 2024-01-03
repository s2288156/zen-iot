package org.zeniot.transport;

import lombok.SneakyThrows;
import org.zeniot.transport.etherip.EtherNetIPClient;

import java.util.concurrent.TimeUnit;

/**
 * @author Jack Wu
 */
public class EtherNetTest {
    @SneakyThrows
    public static void main(String[] args) {
        EtherNetIPClient client = new EtherNetIPClient("172.22.252.207", 2);
        client.connectTcp();
        TimeUnit.SECONDS.sleep(3);
        client.disconnect();
    }
}
