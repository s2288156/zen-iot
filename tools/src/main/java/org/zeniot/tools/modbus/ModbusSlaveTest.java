package org.zeniot.tools.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class ModbusSlaveTest {
    public static void main(String[] args) throws UnknownHostException, ModbusException {
        ModbusSlave tcpSlave = ModbusSlaveFactory.createTCPSlave(InetAddress.getByName("127.0.0.1"), 502, 20, false);
        SimpleProcessImage spi = new SimpleProcessImage(1);
        spi.addRegister(1, new SimpleInputRegister(30));
        spi.addInputRegister(new SimpleInputRegister(45));

        tcpSlave.addProcessImage(1, spi);

        new Thread(() -> {
            try {
                tcpSlave.open();
            } catch (ModbusException e) {
                log.error("", e);
            }
        }).start();

        log.warn("opened ...");
    }
}
