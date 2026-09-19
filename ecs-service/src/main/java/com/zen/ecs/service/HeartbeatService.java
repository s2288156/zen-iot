package com.zen.ecs.service;

import com.zen.common.security.error.BusinessException;
import com.zen.ecs.config.ZenEcsProperties;
import com.zen.ecs.entity.DeviceEntity;
import com.zen.ecs.entity.DeviceEventEntity;
import com.zen.ecs.entity.DeviceEventType;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.presence.DevicePresenceStore;
import com.zen.ecs.repository.DeviceEventRepository;
import com.zen.ecs.repository.DeviceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 心跳：上报只往 {@link DevicePresenceStore} 记一次「活着」，在线/离线一律由 {@link #detectTimeouts()} 按注入的
 * {@link Clock} 判定。数据库里的 {@code online} 因此是判定结果，不是上报的直接产物。
 *
 * <p>时钟必须注入：把时钟拨过阈值就能测「超时自动离线」，不必真等 30 秒，也不会让测试依赖机器时间。
 */
@Service
public class HeartbeatService {

    private final DeviceRepository deviceRepository;

    private final DeviceEventRepository deviceEventRepository;

    private final DevicePresenceStore presenceStore;

    private final ZenEcsProperties properties;

    private final Clock clock;

    public HeartbeatService(
            DeviceRepository deviceRepository,
            DeviceEventRepository deviceEventRepository,
            DevicePresenceStore presenceStore,
            ZenEcsProperties properties,
            Clock clock) {
        this.deviceRepository = deviceRepository;
        this.deviceEventRepository = deviceEventRepository;
        this.presenceStore = presenceStore;
        this.properties = properties;
        this.clock = clock;
    }

    /** 记一次心跳；原本离线的设备借此转在线并留下 ONLINE 事件。按编码上报：设备不知道我们的主键。 */
    @Transactional
    public void report(String deviceCode) {
        DeviceEntity device = deviceRepository.findByDeviceCode(deviceCode).orElseThrow(() -> notFound(deviceCode));
        Instant seenAt = clock.instant();
        // TTL 就等于超时阈值：键自然过期与「键还在但太旧」必须是同一个结论，否则两个判据互相矛盾
        presenceStore.markAlive(
                device.getId(), seenAt, properties.getHeartbeat().getTimeout());
        device.setLastHeartbeatTime(at(seenAt));
        if (!device.isOnline()) {
            device.markOnline(true);
            deviceEventRepository.save(
                    DeviceEventEntity.of(device.getId(), DeviceEventType.ONLINE, at(seenAt), "心跳上报"));
        }
    }

    /**
     * 把「超过 timeout 没心跳」的在线设备置为离线并留下 OFFLINE 事件，返回本次判定的台数供探测任务记日志。
     *
     * <p>判定为离线后清键：否则设备持续掉线会让每个探测周期都重复触发同一条离线事件。
     */
    @Transactional
    public int detectTimeouts() {
        Instant now = clock.instant();
        Instant deadline = now.minus(properties.getHeartbeat().getTimeout());
        List<DeviceEntity> onlineDevices = deviceRepository.findByOnline(DeviceEntity.ONLINE);
        int offline = 0;
        for (DeviceEntity device : onlineDevices) {
            Optional<Instant> lastSeen = presenceStore.lastSeen(device.getId());
            if (lastSeen.isPresent() && lastSeen.get().isAfter(deadline)) {
                continue;
            }
            device.markOnline(false);
            deviceEventRepository.save(DeviceEventEntity.of(device.getId(), DeviceEventType.OFFLINE, at(now), "心跳超时"));
            presenceStore.clear(device.getId());
            offline++;
        }
        return offline;
    }

    private BusinessException notFound(String deviceCode) {
        return new BusinessException(EcsErrorCode.DEVICE_NOT_FOUND, "设备编码 " + deviceCode + " 不存在");
    }

    private LocalDateTime at(Instant instant) {
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }
}
