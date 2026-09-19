package com.zen.ecs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zen.ecs.config.ZenEcsProperties;
import com.zen.ecs.dto.RcsCommandMessage;
import com.zen.ecs.entity.CommandStatus;
import com.zen.ecs.entity.DeviceCommandEntity;
import com.zen.ecs.entity.DeviceEntity;
import com.zen.ecs.protocol.AdapterStatus;
import com.zen.ecs.protocol.CommandOutcome;
import com.zen.ecs.protocol.DeviceAdapter;
import com.zen.ecs.protocol.DeviceAdapterFactory;
import com.zen.ecs.protocol.DeviceTarget;
import com.zen.ecs.protocol.ProtocolSupport;
import com.zen.ecs.repository.DeviceCommandRepository;
import com.zen.ecs.repository.DeviceRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 指令中转的判定：设备接受、设备拒绝、连不上/超时可重试、重复投递不再执行。
 *
 * <p>假适配器替掉真实协议，超时用毫秒级等待测——「超时能触发重试」这条验收标准不该靠真等 5 秒来证明。
 */
class CommandDispatchServiceTest {

    private static final Instant START = Instant.parse("2026-09-19T08:00:00Z");

    private static final String COMMAND_NO = "RC-2026-0001";

    private static final String DEVICE_CODE = "PLC-01";

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private final DeviceCommandRepository deviceCommandRepository = mock(DeviceCommandRepository.class);

    private final ZenEcsProperties properties = new ZenEcsProperties();

    private final FakeAdapter adapter = new FakeAdapter();

    private final Clock clock = Clock.fixed(START, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        properties.getCommand().setTimeout(Duration.ofMillis(200));
        properties.getCommand().setMaxAttempts(3);
        when(deviceRepository.findByDeviceCode(DEVICE_CODE)).thenReturn(Optional.of(device("loopback")));
        // 真实 save 会返回带主键的受管实体，中转要靠这个返回值写终态
        when(deviceCommandRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void acceptedWriteFinishesCommandAsSuccess() {
        adapter.outcome = CommandOutcome.accepted("MOVE=pos-3");

        service().dispatch(command());

        DeviceCommandEntity finished = lastSaved();
        assertThat(finished.getStatus()).isEqualTo(CommandStatus.SUCCESS);
        assertThat(finished.getRetryCount()).isZero();
        assertThat(finished.getErrorMessage()).isNull();
        assertThat(finished.getFinishTime()).isEqualTo(LocalDateTime.ofInstant(START, ZoneOffset.UTC));
        assertThat(finished.getCommandNo()).isEqualTo(COMMAND_NO);
        assertThat(finished.getDeviceId()).isEqualTo(1L);
        assertThat(adapter.writeCount()).isEqualTo(1);
    }

    @Test
    void deviceRejectionIsNotRetried() {
        adapter.outcome = CommandOutcome.rejected("门锁未开");

        service().dispatch(command());

        DeviceCommandEntity finished = lastSaved();
        assertThat(finished.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(finished.getErrorMessage()).contains("门锁未开");
        assertThat(finished.getRetryCount()).isZero();
        // 拒绝是设备给出的结论，重试只会把同一条被拒指令再发一遍
        assertThat(adapter.writeCount()).isEqualTo(1);
    }

    @Test
    void connectionFailureIsRetriedUntilAttemptsAreExhausted() {
        adapter.connectFailure = new IllegalStateException("connection refused");

        service().dispatch(command());

        DeviceCommandEntity finished = lastSaved();
        assertThat(finished.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(finished.getErrorMessage()).contains("connection refused");
        assertThat(finished.getRetryCount()).isEqualTo(2);
        assertThat(adapter.connectCount()).isEqualTo(3);
        assertThat(adapter.writeCount()).isZero();
    }

    /** 超时既要把指令判成失败，也要真的中断卡住的工作线程——否则线程会一直挂在 socket 上。 */
    @Test
    void hangingDeviceTimesOutAndRetries() throws Exception {
        adapter.blocksForever = true;

        service().dispatch(command());

        DeviceCommandEntity finished = lastSaved();
        assertThat(finished.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(finished.getErrorMessage()).contains("执行超时");
        assertThat(finished.getRetryCount()).isEqualTo(2);
        assertThat(adapter.interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void duplicatedCommandIsNotExecutedTwice() {
        when(deviceCommandRepository.findByCommandNo(COMMAND_NO))
                .thenReturn(Optional.of(DeviceCommandEntity.pending(COMMAND_NO, 1L, "MOVE", "pos-3")));

        service().dispatch(command());

        assertThat(adapter.connectCount()).isZero();
        verify(deviceCommandRepository, never()).save(any());
    }

    /** 并发重投时「先查后插」会双双漏过，唯一索引才是裁判：撞键按重复处理，不抛给消费者。 */
    @Test
    void uniqueKeyConflictIsTreatedAsDuplicateInsteadOfBlowingUp() {
        when(deviceCommandRepository.save(any())).thenThrow(new DataIntegrityViolationException("uk_command_no"));

        assertThatNoException().isThrownBy(() -> service().dispatch(command()));

        assertThat(adapter.connectCount()).isZero();
    }

    @Test
    void unknownDeviceCodeLeavesFailedTraceWithoutTouchingAnyDevice() {
        when(deviceRepository.findByDeviceCode(DEVICE_CODE)).thenReturn(Optional.empty());

        service().dispatch(command());

        DeviceCommandEntity finished = lastSaved();
        assertThat(finished.getDeviceId()).isNull();
        assertThat(finished.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(finished.getErrorMessage()).contains("不存在");
        assertThat(adapter.connectCount()).isZero();
    }

    /** 幂等键缺失时既无法判重也无处留痕，只能丢弃：抛异常会让消息被无限重投。 */
    @Test
    void messageWithoutCommandNoIsDropped() {
        service().dispatch(new RcsCommandMessage(null, DEVICE_CODE, "MOVE", "pos-3"));

        verify(deviceCommandRepository, never()).save(any());
        verify(deviceRepository, never()).findByDeviceCode(anyString());
    }

    /** 协议无人认领时指令以失败终态收场，而不是被静默丢弃。 */
    @Test
    void unsupportedProtocolEndsAsFailedCommand() {
        when(deviceRepository.findByDeviceCode(DEVICE_CODE)).thenReturn(Optional.of(device("modbus-tcp")));

        service().dispatch(command());

        DeviceCommandEntity finished = lastSaved();
        assertThat(finished.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(finished.getErrorMessage()).contains("modbus-tcp");
        assertThat(adapter.connectCount()).isZero();
    }

    private DeviceEntity device(String protocolType) {
        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        device.setDeviceCode(DEVICE_CODE);
        device.setDeviceName("1 号光栅");
        device.setProtocolType(protocolType);
        device.setEndpoint("loopback://" + DEVICE_CODE);
        device.markOnline(false);
        return device;
    }

    private RcsCommandMessage command() {
        return new RcsCommandMessage(COMMAND_NO, DEVICE_CODE, "MOVE", "pos-3");
    }

    private CommandDispatchService service() {
        DeviceAdapterFactory factory = new DeviceAdapterFactory(List.of(new FakeProtocolSupport()));
        return new CommandDispatchService(deviceRepository, deviceCommandRepository, factory, properties, clock);
    }

    private DeviceCommandEntity lastSaved() {
        ArgumentCaptor<DeviceCommandEntity> captor = ArgumentCaptor.forClass(DeviceCommandEntity.class);
        verify(deviceCommandRepository, atLeastOnce()).save(captor.capture());
        List<DeviceCommandEntity> saved = captor.getAllValues();
        return saved.get(saved.size() - 1);
    }

    /** 只认 loopback，把假适配器交出去；协议字段不在白名单里的设备一律拿不到适配器。 */
    private final class FakeProtocolSupport implements ProtocolSupport {

        @Override
        public boolean supports(String protocolType) {
            return "loopback".equals(protocolType);
        }

        @Override
        public DeviceAdapter open(DeviceTarget target) {
            return adapter;
        }
    }

    /** 可编排的假设备：接受/拒绝、连不上、卡住不返回，外加「被中断」的观测点。 */
    private static class FakeAdapter implements DeviceAdapter {

        /** 中观察点：等待方判定超时后必须真的把工作线程中断掉。 */
        private final CountDownLatch interrupted = new CountDownLatch(1);

        private final AtomicInteger connects = new AtomicInteger();

        private final AtomicInteger writes = new AtomicInteger();

        private CommandOutcome outcome = CommandOutcome.accepted("ok");

        private RuntimeException connectFailure;

        private boolean blocksForever;

        @Override
        public void connect() {
            connects.incrementAndGet();
            if (connectFailure != null) {
                throw connectFailure;
            }
        }

        @Override
        public AdapterStatus status() {
            return new AdapterStatus(true, "fake");
        }

        @Override
        public String read() {
            return outcome.detail();
        }

        @Override
        public CommandOutcome write(String commandType, String payload) {
            writes.incrementAndGet();
            if (blocksForever) {
                awaitInterrupt();
                return CommandOutcome.accepted("迟到的写入");
            }
            return outcome;
        }

        /** 一个永不归零的闩：只有中断才会让它退出，正好模拟卡在 socket 上的真实设备。 */
        private void awaitInterrupt() {
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void close() {
            // 假适配器没有资源可释放
        }

        private int connectCount() {
            return connects.get();
        }

        private int writeCount() {
            return writes.get();
        }
    }
}
