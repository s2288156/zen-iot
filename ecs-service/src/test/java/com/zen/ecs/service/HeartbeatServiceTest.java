package com.zen.ecs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 心跳与超时判定：时钟可拨、在线存储换成内存假实现，全程不碰中间件。
 *
 * <p>这条验收标准（超时自动离线）只能用「把时钟推过阈值」来测；真等 30 秒的测试会在 CI 上随机红。
 */
class HeartbeatServiceTest {

    private static final Instant START = Instant.parse("2026-09-19T08:00:00Z");

    private static final String DEVICE_CODE = "PLC-01";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private final DeviceEventRepository deviceEventRepository = mock(DeviceEventRepository.class);

    private final ZenEcsProperties properties = new ZenEcsProperties();

    private final FakePresenceStore presenceStore = new FakePresenceStore();

    private DeviceEntity device;

    private Clock clock = Clock.fixed(START, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        properties.getHeartbeat().setTimeout(TIMEOUT);
        device = new DeviceEntity();
        device.setId(1L);
        device.setDeviceCode(DEVICE_CODE);
        device.setDeviceName("1 号光栅");
        device.setProtocolType("loopback");
        device.setEndpoint("loopback://" + DEVICE_CODE);
        device.markOnline(false);
        when(deviceRepository.findByDeviceCode(DEVICE_CODE)).thenReturn(Optional.of(device));
        when(deviceRepository.findByOnline(DeviceEntity.ONLINE)).thenReturn(List.of(device));
    }

    @Test
    void firstHeartbeatTakesDeviceOnlineAndRecordsOnlineEvent() {
        service().report(DEVICE_CODE);

        assertThat(device.isOnline()).isTrue();
        assertThat(device.getLastHeartbeatTime()).isEqualTo(LocalDateTime.ofInstant(START, ZoneOffset.UTC));
        assertThat(presenceStore.lastSeen(1L)).contains(START);
        assertThat(recordedEventTypes()).containsExactly(DeviceEventType.ONLINE);
        assertEvent(lastRecordedEvent(), DeviceEventType.ONLINE, START, "心跳上报");
    }

    @Test
    void heartbeatOnAnAlreadyOnlineDeviceDoesNotRecordASecondOnlineEvent() {
        device.markOnline(true);

        service().report(DEVICE_CODE);

        verify(deviceEventRepository, never()).save(any());
    }

    @Test
    void reportForUnknownDeviceCodeFailsAsBusinessError() {
        when(deviceRepository.findByDeviceCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().report("NOPE"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(EcsErrorCode.DEVICE_NOT_FOUND);
    }

    @Test
    void silencePastTimeoutTurnsDeviceOfflineAndClearsPresence() {
        service().report(DEVICE_CODE);

        advance(TIMEOUT.plusSeconds(1));
        int offline = service().detectTimeouts();

        assertThat(offline).isEqualTo(1);
        assertThat(device.isOnline()).isFalse();
        // 清键是为了让持续掉线的设备只在第一个周期触发一次离线事件，而不是每个周期都记一条
        assertThat(presenceStore.lastSeen(1L)).isEmpty();
        assertThat(recordedEventTypes()).containsExactly(DeviceEventType.ONLINE, DeviceEventType.OFFLINE);
        assertEvent(
                lastRecordedEvent(),
                DeviceEventType.OFFLINE,
                START.plus(TIMEOUT).plusSeconds(1),
                "心跳超时");
    }

    @Test
    void heartbeatInsideTimeoutKeepsDeviceOnline() {
        service().report(DEVICE_CODE);

        advance(TIMEOUT.minusSeconds(1));
        int offline = service().detectTimeouts();

        assertThat(offline).isZero();
        assertThat(device.isOnline()).isTrue();
        // 阈值之内不产生任何新事件：唯一的一条还是上报当次写的 ONLINE
        assertThat(recordedEventTypes()).containsExactly(DeviceEventType.ONLINE);
    }

    /** 心跳键缺席等于 Redis 已按 TTL 过期，与「键还在但太旧」判成同一件事。 */
    @Test
    void onlineDeviceWithoutAnyPresenceKeyIsMarkedOffline() {
        device.markOnline(true);

        int offline = service().detectTimeouts();

        assertThat(offline).isEqualTo(1);
        assertThat(device.isOnline()).isFalse();
        assertThat(recordedEventTypes()).containsExactly(DeviceEventType.OFFLINE);
        assertEvent(lastRecordedEvent(), DeviceEventType.OFFLINE, START, "心跳超时");
    }

    private void advance(Duration duration) {
        clock = Clock.offset(clock, duration);
    }

    private HeartbeatService service() {
        return new HeartbeatService(deviceRepository, deviceEventRepository, presenceStore, properties, clock);
    }

    private List<DeviceEventType> recordedEventTypes() {
        return recordedEvents().stream().map(DeviceEventEntity::getEventType).toList();
    }

    private DeviceEventEntity lastRecordedEvent() {
        List<DeviceEventEntity> events = recordedEvents();
        return events.get(events.size() - 1);
    }

    private List<DeviceEventEntity> recordedEvents() {
        ArgumentCaptor<DeviceEventEntity> captor = ArgumentCaptor.forClass(DeviceEventEntity.class);
        verify(deviceEventRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private void assertEvent(DeviceEventEntity event, DeviceEventType type, Instant occurredAt, String reason) {
        assertThat(event.getEventType()).isEqualTo(type);
        assertThat(event.getOccurredAt()).isEqualTo(LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC));
        assertThat(event.getReason()).isEqualTo(reason);
        assertThat(event.getDeviceId()).isEqualTo(1L);
    }

    /**
     * 内存假实现：只回答「见过 / 没见过」。TTL 的自然过期由 Redis 负责（另有集成测试覆盖），
     * 这里缺席就等价于过期，两件事在 service 看来是同一个输入。
     */
    private static class FakePresenceStore implements DevicePresenceStore {

        private final Map<Long, Instant> seen = new HashMap<>();

        @Override
        public void markAlive(long deviceId, Instant seenAt, Duration ttl) {
            seen.put(deviceId, seenAt);
        }

        @Override
        public Optional<Instant> lastSeen(long deviceId) {
            return Optional.ofNullable(seen.get(deviceId));
        }

        @Override
        public void clear(long deviceId) {
            seen.remove(deviceId);
        }
    }
}
