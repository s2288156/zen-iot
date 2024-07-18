package org.zen.iot.tools.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class ModbusMasterTest {
    public static void main(String[] args) {
        try {
            ModbusTCPMaster master = new ModbusTCPMaster("127.0.0.1", 504);
            master.connect();

            log.warn("connected ...");
            new Thread(() -> {
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    try {
                        master.writeSingleRegister(1, 21, new SimpleInputRegister(i % 5));
                        master.writeSingleRegister(1, 20, new SimpleInputRegister(i % 6));
                        master.writeSingleRegister(1, 19, new SimpleInputRegister(i % 7));
                        master.writeSingleRegister(1, 18, new SimpleInputRegister(i % 8));
                        master.writeSingleRegister(1, 17, new SimpleInputRegister(i % 9));
                        master.writeSingleRegister(1, 16, new SimpleInputRegister(i % 10));
                        master.writeSingleRegister(1, 15, new SimpleInputRegister(i % 11));
                        master.writeSingleRegister(1, 14, new SimpleInputRegister(i % 12));
                        master.writeSingleRegister(1, 13, new SimpleInputRegister(i % 13));
                        master.writeSingleRegister(1, 12, new SimpleInputRegister(i % 14));
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (ModbusException | InterruptedException e) {
                        log.error("", e);
                    }
                }
            }).start();
            TimeUnit.SECONDS.sleep(30);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
