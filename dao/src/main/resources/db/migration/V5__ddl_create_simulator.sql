CREATE TABLE t_simulator
(
    id          int8        NOT NULL,
    "name"      varchar(32) NOT NULL,
    config_info json        NULL,
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    CONSTRAINT t_simulator_pk PRIMARY KEY (id)
);
COMMENT ON TABLE t_simulator IS '模拟器';
