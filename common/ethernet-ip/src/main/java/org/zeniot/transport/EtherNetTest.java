package org.zeniot.transport;

import lombok.SneakyThrows;
import org.zeniot.transport.etherip.EtherNetIPClient;
import org.zeniot.transport.etherip.nio.protocol.CIPData;

/**
 * @author Jack Wu
 */
public class EtherNetTest {
    private static final String BTN_START = "Btn_Start";
    private static final String BTN_STOP = "Btn_Stop";
    private static final String MOTOR_CTRL = "MotorCtrl";

    private static String address = "172.22.252.207";
    private static int slot = 2;
    private static short array = 1;
    private static String tag = BTN_STOP;
    private static CIPData write = null;
    @SneakyThrows
    public static void main(String[] args) {
        try (EtherNetIPClient client = new EtherNetIPClient("172.22.252.207", 2)) {
            client.connectTcp();
            System.out.println(client.readTag(MOTOR_CTRL, array));
        }
    }
}
