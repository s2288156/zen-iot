package org.zeniot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;

import javax.persistence.*;

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
    private DeviceTransportTypeEnum transportType;

    @Type(type = "jsonp")
    @Column(name = "transport_config")
    private JsonNode transportConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatusEnum status;

}
