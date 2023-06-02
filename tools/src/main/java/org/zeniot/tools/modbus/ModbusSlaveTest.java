package org.zeniot.tools.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class ModbusSlaveTest {
    public static void main(String[] args) throws UnknownHostException, ModbusException, InterruptedException {
        ModbusSlave slave = ModbusSlaveFactory.createTCPSlave(InetAddress.getByName("127.0.0.1"), 502, 1, false);
        SimpleProcessImage spi = new SimpleProcessImage(1);
        for (int i = 1; i < 22; i++) {
            spi.addRegister(i, new SimpleInputRegister(0));
        }
        spi.setRegister(3, new SimpleInputRegister(1));

        slave.addProcessImage(1, spi);

        new Thread(() -> {
            try {
                slave.open();

            } catch (ModbusException e) {
                log.error("", e);
            }
        }).start();
        new Thread(() -> {
            for (int i = 0; i < 100000; i++) {
                spi.setRegister(21, new SimpleInputRegister(i % 128));
                spi.setRegister(20, new SimpleInputRegister(i % 128));
                spi.setRegister(19, new SimpleInputRegister(i % 128));
                spi.setRegister(18, new SimpleInputRegister(i % 128));
                spi.setRegister(17, new SimpleInputRegister(i % 128));
                spi.setRegister(16, new SimpleInputRegister(i % 128));
                spi.setRegister(15, new SimpleInputRegister(i % 128));
                spi.setRegister(14, new SimpleInputRegister(i % 128));
                spi.setRegister(13, new SimpleInputRegister(i % 128));
                spi.setRegister(12, new SimpleInputRegister(i % 128));
                spi.setRegister(11, new SimpleInputRegister(i % 128));
                spi.setRegister(10, new SimpleInputRegister(i % 128));
                spi.setRegister(9, new SimpleInputRegister(i % 128));
                spi.setRegister(8, new SimpleInputRegister(i % 128));
                spi.setRegister(7, new SimpleInputRegister(i % 128));
                spi.setRegister(6, new SimpleInputRegister(i % 128));
                spi.setRegister(5, new SimpleInputRegister(i % 128));
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }
        }).start();


        log.warn("opened ...");
        new Thread(() -> {
            while (!Thread.interrupted()) {
                Scanner scanner = new Scanner(System.in);
                String cmd = scanner.nextLine();
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < 22; i++) {
                    sb.append("[").append(i).append("]:").append(spi.getRegister(i)).append("/ ");
                }
                System.out.println(sb);
                spi.setRegister(Integer.parseInt(cmd), new SimpleInputRegister(0));
                System.out.println("重置后: " + spi.getRegister(Integer.parseInt(cmd)));
            }
        }).start();

    }
}
