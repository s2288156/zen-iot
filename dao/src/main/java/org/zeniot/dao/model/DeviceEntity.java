package org.zeniot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.TransportTypeEnum;

@AllArgsConstructor
@NoArgsConstructor
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

}
