package org.zeniot.transport.modbus;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Component
public class DefaultModbusTransportService implements ModbusTransportService {

    private List<ModbusClient> modbusClients;

}
