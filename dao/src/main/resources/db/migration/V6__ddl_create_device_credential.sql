CREATE TABLE t_device_credential
(
    id               int8         NOT NULL,
    create_time      timestamp    NOT NULL,
    update_time      timestamp    NOT NULL,
    credential_type  varchar(32)  NOT NULL,
    credential_value varchar(256) NOT NULL,
    device_id        int8         NOT NULL,
    CONSTRAINT t_device_credential_pk PRIMARY KEY (id)
);

-- Column comments

COMMENT ON COLUMN public.t_device_credential.credential_type IS '证书类型：ACCESS_TOKEN';
ALTER TABLE public.t_device_credential
    ADD CONSTRAINT device_id_uk UNIQUE (device_id);
