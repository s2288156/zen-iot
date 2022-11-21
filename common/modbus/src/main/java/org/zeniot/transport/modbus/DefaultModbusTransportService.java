package org.zeniot.transport.modbus;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransport;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Component
public class DefaultModbusTransportService implements ModbusTransportService {

    private List<ModbusClient> modbusClients;

    @SneakyThrows
    public static void main(String[] args) {
        String host = "127.0.0.1";
        Socket socket = new Socket(host, 502);
        ModbusTCPTransport transport = new ModbusTCPTransport(socket);
        ModbusTransaction trans = transport.createTransaction();
        ModbusRequest req = new ReadMultipleRegistersRequest(2, 1);
        req.setUnitID(1);

        trans.setRequest(req);
        trans.execute();

        ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) trans.getResponse();
        System.out.println(response.getRegisterValue(0));

        transport.close();
    }
}
