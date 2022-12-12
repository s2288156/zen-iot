CREATE TABLE t_simulator
(
    id          int8        NOT NULL,
    "name"      varchar(32) NOT NULL,
    transport_type   varchar(32) NOT NULL, -- 协议类型：MQTT, MODBUS
    transport_config json       NULL,
    status           varchar(32) NOT NULL, -- 状态: DISABLE, ENABLE
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    CONSTRAINT t_simulator_pk PRIMARY KEY (id)
);
COMMENT ON TABLE t_simulator IS '模拟器';
