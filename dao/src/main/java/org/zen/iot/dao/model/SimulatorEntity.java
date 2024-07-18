package org.zen.iot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.zen.iot.data.enums.SimulatorStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "t_simulator")
public class SimulatorEntity extends BaseEntity {
    @Column(name = "`name`")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    private TransportTypeEnum transportType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transport_config")
    private JsonNode transportConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SimulatorStatusEnum status;
}
