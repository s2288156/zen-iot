package com.zen.ecs.service;

import com.zen.common.security.error.BusinessException;
import com.zen.ecs.config.ZenEcsProperties;
import com.zen.ecs.dto.CommandResponse;
import com.zen.ecs.dto.RcsCommandMessage;
import com.zen.ecs.entity.CommandStatus;
import com.zen.ecs.entity.DeviceCommandEntity;
import com.zen.ecs.entity.DeviceEntity;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.protocol.CommandOutcome;
import com.zen.ecs.protocol.DeviceAdapter;
import com.zen.ecs.protocol.DeviceAdapterFactory;
import com.zen.ecs.protocol.DeviceTarget;
import com.zen.ecs.repository.DeviceCommandRepository;
import com.zen.ecs.repository.DeviceRepository;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 指令中转：把 RCS 经 MQ 下发的指令路由到设备适配器执行，支持超时与重试。
 *
 * <p>刻意不标 {@code @Transactional}：一次中转最坏要等 尝试次数 × 单次超时，把设备 I/O 圈进数据库事务会长时间占住连接。
 * 落库因此拆成「insert PENDING → 执行 → update 终态」，每一步走 Spring Data 自己的短事务；进程中途被杀留下的 PENDING
 * 行也是可排查的事实，而不是脏数据。
 */
@Slf4j
@Service
public class CommandDispatchService {

    /** error_message 的列宽。异常文本可能远超它，落库前必须截断，否则 MySQL 严格模式会把终态写入本身打成失败。 */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 255;

    private final DeviceRepository deviceRepository;

    private final DeviceCommandRepository deviceCommandRepository;

    private final DeviceAdapterFactory adapterFactory;

    private final ZenEcsProperties properties;

    private final Clock clock;

    /**
     * 每次执行放进一个独立的（虚拟）线程，才能在等待方用 {@code Future.get(timeout)} 判定超时并中断它。
     *
     * <p>不注册成 Bean：Boot 的 {@code applicationTaskExecutor} 带
     * {@code @ConditionalOnMissingBean({Executor.class, ExecutorService.class})}，本模块一暴露这类 Bean 就把它顶掉了。
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CommandDispatchService(
            DeviceRepository deviceRepository,
            DeviceCommandRepository deviceCommandRepository,
            DeviceAdapterFactory adapterFactory,
            ZenEcsProperties properties,
            Clock clock) {
        this.deviceRepository = deviceRepository;
        this.deviceCommandRepository = deviceCommandRepository;
        this.adapterFactory = adapterFactory;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 消费一条 RCS 指令。幂等键是 {@code commandNo}：MQ 至少一次投递，重复投递不得再碰设备第二次。
     *
     * <p>设备编码查无此设备时只留痕（{@code deviceId} 为空、直接终态 FAILED）而不抛异常：抛出去会让消息被重投，
     * 一条指向不存在设备的消息会把消费者永久卡住。
     */
    public void dispatch(RcsCommandMessage message) {
        if (message == null || isBlank(message.commandNo())) {
            log.error("丢弃缺少 commandNo 的指令消息：幂等键缺失，既不能判重也无处留痕");
            return;
        }
        String commandNo = message.commandNo();
        Optional<DeviceEntity> device = deviceRepository.findByDeviceCode(message.deviceCode());
        DeviceCommandEntity command =
                register(commandNo, message, device.map(DeviceEntity::getId).orElse(null));
        if (command == null) {
            return;
        }
        if (device.isEmpty()) {
            log.warn("指令 {} 的目标设备 {} 不存在，只留痕不执行", commandNo, message.deviceCode());
            finish(command, CommandStatus.FAILED, 0, "设备编码 " + message.deviceCode() + " 不存在");
            return;
        }
        executeWithRetry(command, device.get(), message);
    }

    /** 指令状态回读：与中转打的是同一张表，不另立一个只读 service。 */
    public CommandResponse get(String commandNo) {
        return deviceCommandRepository
                .findByCommandNo(commandNo)
                .map(CommandResponse::from)
                .orElseThrow(() -> new BusinessException(
                        EcsErrorCode.COMMAND_NOT_FOUND, EcsErrorCode.COMMAND_NOT_FOUND.getMessage() + "：" + commandNo));
    }

    /**
     * 插入 PENDING 行；返回 {@code null} 表示这条指令已经中转过，调用方据此跳过。
     *
     * <p>先查后插挡不住两个实例同时消费同一条重投消息，真正的裁判是 {@code uk_command_no}：撞唯一键即按重复处理。
     */
    private DeviceCommandEntity register(String commandNo, RcsCommandMessage message, Long deviceId) {
        if (deviceCommandRepository.findByCommandNo(commandNo).isPresent()) {
            log.info("指令 {} 已中转过，跳过执行", commandNo);
            return null;
        }
        try {
            return deviceCommandRepository.save(
                    DeviceCommandEntity.pending(commandNo, deviceId, message.commandType(), message.payload()));
        } catch (DataIntegrityViolationException e) {
            log.info("指令 {} 已有记录（并发重复投递），跳过执行", commandNo);
            return null;
        }
    }

    private void executeWithRetry(DeviceCommandEntity command, DeviceEntity device, RcsCommandMessage message) {
        int maxAttempts = Math.max(1, properties.getCommand().getMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                CommandOutcome outcome = attemptOnce(device, message);
                if (outcome.accepted()) {
                    log.info("指令 {} 第 {} 次执行被接受：{}", command.getCommandNo(), attempt, outcome.detail());
                    finish(command, CommandStatus.SUCCESS, attempt - 1, null);
                    return;
                }
                // 设备明确拒绝：重试只是把同一条被拒的指令再发一遍
                finish(command, CommandStatus.FAILED, attempt - 1, "设备拒绝：" + outcome.detail());
                return;
            } catch (CommandAttemptException e) {
                log.warn("指令 {} 第 {} 次执行失败：{}", command.getCommandNo(), attempt, e.getMessage());
                if (attempt == maxAttempts) {
                    finish(command, CommandStatus.FAILED, attempt - 1, e.getMessage());
                    return;
                }
            }
        }
    }

    /**
     * 一次执行：连接、写入、关闭都在工作线程里做，等待方只给它 {@code zen.ecs.command.timeout}。
     *
     * @throws CommandAttemptException 这一轮没拿到结论（连不上、超时、被中断）；与「设备拒绝」区分开，前者可重试、后者不可
     */
    private CommandOutcome attemptOnce(DeviceEntity device, RcsCommandMessage message) {
        DeviceTarget target = new DeviceTarget(device.getDeviceCode(), device.getProtocolType(), device.getEndpoint());
        Duration timeout = properties.getCommand().getTimeout();
        Future<CommandOutcome> future = executor.submit(() -> {
            try (DeviceAdapter adapter = adapterFactory.open(target)) {
                adapter.connect();
                return adapter.write(message.commandType(), message.payload());
            }
        });
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 中断工作线程：DeviceAdapter 的实现必须能从中断里退出，否则这条指令的线程会一直挂着
            future.cancel(true);
            throw new CommandAttemptException("执行超时 " + timeout.toMillis() + "ms", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new CommandAttemptException("执行被中断", e);
        } catch (ExecutionException e) {
            throw new CommandAttemptException(Objects.toString(e.getCause(), e.getMessage()), e);
        }
    }

    private void finish(DeviceCommandEntity command, CommandStatus status, int retries, String errorMessage) {
        LocalDateTime finishTime = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        command.finish(status, retries, truncate(errorMessage), finishTime);
        deviceCommandRepository.save(command);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @PreDestroy
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    /** 一轮执行没拿到结论的私有标记，只为把「可重试」与「设备拒绝」在类型上分开。 */
    private static class CommandAttemptException extends RuntimeException {

        CommandAttemptException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
