CREATE TABLE t_account
(
    id          bigint        NOT NULL,
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    pwd  varchar(64) NOT NULL,
    username    varchar(32) NOT NULL,
    CONSTRAINT t_account_pkey PRIMARY KEY (id)
);

CREATE TABLE t_role
(
    id          bigint        NOT NULL,
    create_time timestamp   NOT NULL,
    update_time timestamp   NOT NULL,
    role_name      varchar(64) NOT NULL,
    CONSTRAINT t_role_pkey PRIMARY KEY (id)
);

CREATE TABLE t_account_role
(
    account_id bigint NOT NULL,
    role_id    bigint NOT NULL,
    CONSTRAINT t_account_role_pkey PRIMARY KEY (account_id, role_id)
);
