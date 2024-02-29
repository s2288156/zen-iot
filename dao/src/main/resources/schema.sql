-- public.t_account definition

-- Drop table

-- DROP TABLE t_account;

CREATE TABLE t_account
(
    id          int8        NOT NULL,
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    pwd         varchar(64) NOT NULL,
    username    varchar(32) NOT NULL,
    CONSTRAINT t_account_pkey PRIMARY KEY (id)
);


-- public.t_account_role definition

-- Drop table

-- DROP TABLE t_account_role;

CREATE TABLE t_account_role
(
    account_id int8 NOT NULL,
    role_id    int8 NOT NULL,
    CONSTRAINT t_account_role_pkey PRIMARY KEY (account_id, role_id)
);


-- public.t_device definition

-- Drop table

-- DROP TABLE t_device;

CREATE TABLE t_device
(
    id               int8        NOT NULL,
    create_time      timestamp   NOT NULL,
    update_time      timestamp   NOT NULL,
    "name"           varchar(64) NOT NULL,
    transport_type   varchar(32) NOT NULL,
    transport_config json        NULL,
    status           varchar(32) NOT NULL,
    CONSTRAINT t_device_pkey PRIMARY KEY (id)
);


-- public.t_device_credential definition

-- Drop table

-- DROP TABLE t_device_credential;

CREATE TABLE t_device_credential
(
    id               int8         NOT NULL,
    create_time      timestamp    NOT NULL,
    update_time      timestamp    NOT NULL,
    credential_type  varchar(32)  NOT NULL,
    credential_value varchar(256) NOT NULL,
    device_id        int8         NOT NULL,
    CONSTRAINT device_id_uk UNIQUE (device_id),
    CONSTRAINT t_device_credential_pk PRIMARY KEY (id)
);


-- public.t_node definition

-- Drop table

-- DROP TABLE t_node;

CREATE TABLE t_node
(
    id            varchar(40) NOT NULL,
    create_time   timestamp   NOT NULL,
    update_time   timestamp   NOT NULL,
    node_name     varchar(32) NOT NULL,
    rule_chain_id int8        NOT NULL,
    node_type     varchar(32) NOT NULL,
    metadata      json        NOT NULL,
    CONSTRAINT t_node_pk PRIMARY KEY (id)
);
CREATE INDEX t_node_rule_chain_id_idx ON public.t_node USING btree (rule_chain_id);


-- public.t_node_relation definition

-- Drop table

-- DROP TABLE t_node_relation;

CREATE TABLE t_node_relation
(
    id            varchar(40) NOT NULL,
    create_time   timestamp   NOT NULL,
    update_time   timestamp   NOT NULL,
    source_id     varchar(40) NOT NULL,
    target_id     varchar(40) NOT NULL,
    source_port   varchar(10) NULL,
    target_port   varchar(10) NULL,
    rule_chain_id int8        NOT NULL,
    CONSTRAINT t_node_relation_pk PRIMARY KEY (id)
);
CREATE INDEX t_node_relation_source_id_idx ON public.t_node_relation USING btree (source_id);


-- public.t_role definition

-- Drop table

-- DROP TABLE t_role;

CREATE TABLE t_role
(
    id          int8        NOT NULL,
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    role_name   varchar(64) NOT NULL,
    CONSTRAINT t_role_pkey PRIMARY KEY (id)
);


-- public.t_rule_chain definition

-- Drop table

-- DROP TABLE t_rule_chain;

CREATE TABLE t_rule_chain
(
    id          int8         NOT NULL,
    create_time timestamp    NOT NULL,
    update_time timestamp    NOT NULL,
    "name"      varchar(128) NOT NULL,
    CONSTRAINT t_rule_chain_pk PRIMARY KEY (id)
);


-- public.t_simulator definition

-- Drop table

-- DROP TABLE t_simulator;

CREATE TABLE t_simulator
(
    id               int8        NOT NULL,
    "name"           varchar(32) NOT NULL,
    transport_type   varchar(32) NOT NULL,
    transport_config json        NULL,
    status           varchar(32) NOT NULL,
    create_time      timestamp   NOT NULL,
    update_time      timestamp   NOT NULL,
    CONSTRAINT t_simulator_pk PRIMARY KEY (id)
);
