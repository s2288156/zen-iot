CREATE TABLE t_device
(
    id               int8        NOT NULL,
    create_time      timestamp   NOT NULL,
    update_time      timestamp   NOT NULL,
    "name"           varchar(64) NOT NULL,
    transport_type   varchar(32) NOT NULL, -- 协议类型：MQTT, MODBUS
    transport_config json       NULL,
    status           varchar(32) NOT NULL, -- 设备状态: DISABLE, ENABLE
    CONSTRAINT t_device_pkey PRIMARY KEY (id)
);

-- Column comments

COMMENT ON COLUMN t_device.transport_type IS '协议类型：MQTT, MODBUS';
COMMENT ON COLUMN t_device.status IS '设备状态: DISABLE, ENABLE';
