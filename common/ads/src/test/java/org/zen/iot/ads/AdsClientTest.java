package org.zen.iot.ads;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.zen.iot.ads.packet.AdsDataType;
import org.zen.iot.ads.packet.command.ReadCommand;

import java.util.concurrent.TimeUnit;

/**
 * @author Jack Wu
 */
@Slf4j
class AdsClientTest {

    @Test
    void write() {
        String ip = "10.86.201.153";
        // String ip = "172.29.130.198";

        String sourceAmsNetId = "10.86.195.33.1.1";
        // String sourceAmsNetId = "172.29.128.1.1.1";
        int sourceAmsPort = 851;
        String targetAmsNetId = "192.168.56.1.1.1";
        int targetAmsPort = 851;
        AdsClient adsClient = new AdsClient(ip, 48898, targetAmsNetId, targetAmsPort, sourceAmsNetId, sourceAmsPort);
        // 如果使用属性名称，需要通过一个命令查询到名称和indexOffset的对应关系，最终报文中发送的是indexOffset
        ReadCommand readCommand = new ReadCommand(385000, AdsDataType.BOOL);
        // ReadCommand readCommand = new ReadCommand(385004, AdsDataType.INT);
        adsClient.connect();
        for (int i = 0; i < 1; i++) {
            adsClient.write(readCommand);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }
}
