package org.zeniot.ads;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.zeniot.ads.packet.AdsDataType;
import org.zeniot.ads.packet.command.ReadCommand;

import java.util.concurrent.TimeUnit;

/**
 * @author Jack Wu
 */
@Slf4j
class AdsClientTest {

    @Test
    void write() {
        String sourceAmsNetId = "172.18.16.1.1.1";
        int sourceAmsPort = 851;
        String ip = "172.18.21.43";
        String targetAmsNetId = "172.28.131.49.1.1";
        int targetAmsPort = 851;
        AdsClient adsClient = new AdsClient(ip, 48898, targetAmsNetId, targetAmsPort, sourceAmsNetId, sourceAmsPort);
        ReadCommand readCommand = new ReadCommand(385000, AdsDataType.BOOL);
        adsClient.connect();
        for (int i = 0; i < 10; i++) {
            adsClient.write(readCommand);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }
}
