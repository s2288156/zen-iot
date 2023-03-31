package org.zeniot.dao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.zeniot.data.enums.DeviceCredentialTypeEnum;

@Getter
@Setter
@Entity
@ToString
@Table(name = "t_device_credential")
public class DeviceCredentialEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type")
    private DeviceCredentialTypeEnum credentialType;

    @Column(name = "credential_value")
    private String credentialValue;

    @Column(name = "device_id")
    private Long deviceId;

    @ToString.Exclude
    @OneToOne
    @JoinColumn(name = "device_id", insertable = false, updatable = false)
    private DeviceEntity deviceEntity;

}
