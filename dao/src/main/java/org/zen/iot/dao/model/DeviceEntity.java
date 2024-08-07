package org.zen.iot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.zen.iot.data.enums.DeviceStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

@Getter
@Setter
@ToString
@Entity
@Table(name = "t_device")
public class DeviceEntity extends BaseEntity {

    @Column(name = "name", length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    private TransportTypeEnum transportType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transport_config")
    private JsonNode transportConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatusEnum status;

    @OneToOne(mappedBy = "deviceEntity", orphanRemoval = true)
    private DeviceCredentialEntity deviceCredentialEntity;

}
