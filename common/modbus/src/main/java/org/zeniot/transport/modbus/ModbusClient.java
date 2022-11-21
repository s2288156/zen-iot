package org.zeniot.transport.modbus;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import org.zeniot.data.device.ModbusAttribute;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
public class ModbusClient {
    private ModbusTransaction transaction;
    private String host;
    private int port;

    private List<ModbusAttribute> attributes;

    private ModbusClient() {
    }

    protected void connect() {

    }
}
