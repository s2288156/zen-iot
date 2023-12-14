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
    id          varchar(40) NOT NULL,
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    source_id   varchar(40) NOT NULL,
    target_id   varchar(40) NOT NULL,
    source_port varchar(10) NULL,
    target_port varchar(10) NULL,
    CONSTRAINT t_node_relation_pk PRIMARY KEY (id)
);
CREATE INDEX t_node_relation_source_id_idx ON public.t_node_relation USING btree (source_id);


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
