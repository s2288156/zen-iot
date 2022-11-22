package org.zeniot.data.device;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
public class ModbusTransportConf {
    private String host;
    private int port;
    private int unitId;

    private List<ModbusAttribute> attributes;
}
