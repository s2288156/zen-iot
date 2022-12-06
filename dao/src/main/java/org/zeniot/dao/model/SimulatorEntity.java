package org.zeniot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_info")
    private JsonNode configInfo;
}
