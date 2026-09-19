package com.zen.ecs.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 设备档案：AMR 之外的外部设备（PLC / 光栅 / 自动门 / 充电桩…）。
 *
 * <p>分组只存 {@code groupId} 而不建 JPA 关联：跨表取分组名一律由 service 分别查，避免实体图被序列化到入口层之外。
 *
 * <p>删除是逻辑删除：{@code t_device_event}/{@code t_device_command} 按 {@code deviceId} 引用档案，物理删行会让历史事件查不到主语。
 * 但 {@code uk_device_code} 不区分 {@code deleted}，软删掉的编码仍占着唯一索引，这一点由
 * {@code DeviceService} 在新建时按全表查重并明确报错，而不是留给数据库抛 500。
 */
@Getter
@Setter
@Entity
@Table(name = "t_device")
@SQLRestriction("deleted = 0")
@SQLDelete(sql = "UPDATE t_device SET deleted = 1 WHERE id = ?")
public class DeviceEntity extends BaseEntity {

    /** 在线标记，对应 t_device.online 的 TINYINT；改成 Integer 会让 ddl-auto=validate 报列类型不符。 */
    public static final byte ONLINE = 1;

    public static final byte OFFLINE = 0;

    @Column(name = "device_code", nullable = false, length = 64, unique = true)
    private String deviceCode;

    @Column(name = "device_name", nullable = false, length = 64)
    private String deviceName;

    @Column(name = "group_id")
    private Long groupId;

    /** 协议类型，交给 {@code com.zen.ecs.protocol.DeviceAdapterFactory} 按字符串挑选适配器。 */
    @Column(name = "protocol_type", nullable = false, length = 32)
    private String protocolType;

    /** 设备地址，语义由对应协议定义（如 {@code tcp://127.0.0.1:502/1}），本服务不解析。 */
    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "online", nullable = false)
    private Byte online;

    /** 仅用于展示；超时判定读 Redis 里的最近心跳与注入的 {@code java.time.Clock}，不依赖这一列。 */
    @Column(name = "last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;

    public boolean isOnline() {
        return online != null && online == ONLINE;
    }

    public void markOnline(boolean online) {
        this.online = online ? ONLINE : OFFLINE;
    }
}
