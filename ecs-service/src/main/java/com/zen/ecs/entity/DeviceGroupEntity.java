package com.zen.ecs.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 设备分组：按产线/区域聚合设备，删除前必须确认组内没有设备。
 *
 * <p>与 {@link DeviceEntity} 同样是逻辑删除，理由也相同：{@code uk_group_code} 不含 {@code deleted}，软删后的编码仍占位。
 */
@Getter
@Setter
@Entity
@Table(name = "t_device_group")
@SQLRestriction("deleted = 0")
@SQLDelete(sql = "UPDATE t_device_group SET deleted = 1 WHERE id = ?")
public class DeviceGroupEntity extends BaseEntity {

    @Column(name = "group_code", nullable = false, length = 64, unique = true)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 64)
    private String groupName;

    @Column(name = "description", length = 255)
    private String description;
}
