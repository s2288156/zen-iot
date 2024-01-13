package org.zeniot.transport;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.zeniot.transport.etherip.EtherNetIPClient;
import org.zeniot.transport.etherip.nio.protocol.CIPData;

/**
 * @author Jack Wu
 */
@Slf4j
public class EtherNetTest {
    private static final String TAG_BTN_START = "Btn_Start";
    private static final String TAG_BTN_STOP = "Btn_Stop";
    private static final String TAG_MOTOR_CTRL = "Motor_Ctrl";

    private static String address = "172.26.129.24";
    private static int slot = 2;
    private static short array = 1;
    private static CIPData write = null;
    @SneakyThrows
    public static void main(String[] args) {
        try (EtherNetIPClient client = new EtherNetIPClient(address, slot)) {
            client.connectTcp();
            log.info("{}:{}", TAG_BTN_START, client.readTag(TAG_BTN_START, array));
            log.info("{}:{}", TAG_BTN_STOP, client.readTag(TAG_BTN_STOP, array));
            log.info("{}:{}", TAG_MOTOR_CTRL, client.readTag(TAG_MOTOR_CTRL, array));
        }
    }
}
