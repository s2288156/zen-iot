package org.zeniot.ads;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.zeniot.common.util.DataTypeConvertor;

import java.util.concurrent.TimeUnit;

/**
 * @author Jack Wu
 */
@Slf4j
public class AdsClientDemo {


    public static void main(String[] args) {
        request();
        // t1();
    }

    private static void t1() {
        String hex = "00155d1f640b00155d02e96008004500005ae55140003f067ed3ac14dd65ac12a1ec8f16bf0213d455b63d018720501801f6e90d000000002c000000ac1c833101015303ac14dd6501015303020004000c000000000000009e66050040400000e8df050001000000";
        byte[] request = DataTypeConvertor.hexToBytes(hex);
        int[] values = new int[request.length];
        for (int i = 0; i < request.length; i++) {
            values[i] = Byte.valueOf(request[i]).intValue();
        }
        log.info("{}", values);
    }

    @SneakyThrows
    private static void request() {
        String sourceAmsNetId = "172.18.160.1.1.1";
        int sourceAmsPort = 851;
        String ip = "172.18.21.43";
        String targetAmsNetId = "172.28.131.49.1.1";
        int targetAmsPort = 851;
        AdsClient adsClient = new AdsClient(ip, 48898, targetAmsNetId, targetAmsPort, sourceAmsNetId, sourceAmsPort);
        adsClient.connect();
        byte[] request = {
                (byte) 0x00, (byte) 0x00, (byte) 0x2c, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 6 bit
                (byte) 0xac, (byte) 0x1c, (byte) 0x83, (byte) 0x31, (byte) 0x01, (byte) 0x01, (byte) 0x53, (byte) 0x03, // 8 bit target: 172.18.161.236:851
                (byte) 0xac, (byte) 0x12, (byte) 0xa0, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x53, (byte) 0x03, // 8 bit source
                (byte) 0x02, (byte) 0x00, (byte) 0x04, (byte) 0x00, // command id , state flag
                (byte) 0x0c, (byte) 0x00, (byte) 0x00, (byte) 0x00, // length
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // error code
                (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x01, // invoke id
                (byte) 0x40, (byte) 0x40, (byte) 0x00, (byte) 0x01, (byte) 0xe8, (byte) 0xdf, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,// data
        };
        for (int i = 0; i < 3; i++) {
            adsClient.writeMsg(request);
            TimeUnit.SECONDS.sleep(1);
        }
        log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }

    private static byte toByte(Integer v) {
        return v.byteValue();
    }

    private static byte[] toPort(int port) {
        byte[] result = new byte[2];
        result[1] = (byte) ((port >> 8) & 0xFF);
        result[0] = (byte) (port & 0xFF);
        return result;
    }

}
