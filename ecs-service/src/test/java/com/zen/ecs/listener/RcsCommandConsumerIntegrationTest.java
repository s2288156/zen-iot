package com.zen.ecs.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.ecs.config.ZenEcsProperties;
import com.zen.ecs.dto.DeviceCreateRequest;
import com.zen.ecs.dto.DeviceResponse;
import com.zen.ecs.dto.RcsCommandMessage;
import com.zen.ecs.entity.CommandStatus;
import com.zen.ecs.entity.DeviceCommandEntity;
import com.zen.ecs.protocol.LoopbackProtocolSupport;
import com.zen.ecs.repository.DeviceCommandRepository;
import com.zen.ecs.repository.DeviceRepository;
import com.zen.ecs.service.DeviceService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 指令通道的端到端切片：以 RCS 的身份往 exchange 投一条 JSON，验证本服务侧「绑定生效 → 反序列化 → 中转执行 → 回写终态」整条链。
 *
 * <p>这些环节各自都有单元测试，但单元测试挡不住它们**接起来**的失败：exchange / queue / routing key 拼错会让消息静默落到
 * 无人消费的地方（投递方照样返回成功），受信包白名单收严后反序列化会在消费者线程里炸，{@code @RabbitListener} 的队列名
 * 占位符写错则根本没人监听。这类失败只在真 broker 上暴露。
 *
 * <p>不带 {@code @Transactional}：消费者在另一个线程、另一个事务里写库，测试事务既看不见它的提交也替它回滚不了，
 * 所以探针数据由 {@link #removeProbeRows()} 自己清。设备编码与指令号都带随机后缀，即使清理失败也不会占住唯一索引。
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration") // 依赖本机 MySQL/Redis/RabbitMQ
class RcsCommandConsumerIntegrationTest {

    /** 消费者是异步的，给它落终态的上限；正常几百毫秒内就到，超时说明链路断了。 */
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(15);

    private static final long POLL_INTERVAL_MILLIS = 100L;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ZenEcsProperties properties;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceCommandRepository deviceCommandRepository;

    private String deviceCode;

    private final List<String> issuedCommandNos = new ArrayList<>();

    @BeforeEach
    void pickUniqueIdentifiers() {
        // 唯一后缀：uk_device_code 不区分软删除，撞上一个测试跑剩的编码会得到 1002 而不是真实结论
        deviceCode = "it-" + UUID.randomUUID();
    }

    @AfterEach
    void removeProbeRows() {
        for (String commandNo : issuedCommandNos) {
            deviceCommandRepository
                    .findByCommandNo(commandNo)
                    .ifPresent(command -> deviceCommandRepository.deleteById(command.getId()));
        }
        // 档案是软删除（实体上的 @SQLDelete），行会留着，但编码不会再被下一个测试撞上
        deviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> deviceRepository.deleteById(device.getId()));
    }

    @Test
    void commandDeliveredOverRabbitIsExecutedAndSettled() {
        DeviceResponse device = createLoopbackDevice();

        String commandNo = publish("MOVE", device.deviceCode(), "target=station-3");

        DeviceCommandEntity settled = awaitSettled(commandNo);
        assertThat(settled.getStatus()).isEqualTo(CommandStatus.SUCCESS);
        assertThat(settled.getDeviceId()).isEqualTo(device.id());
        assertThat(settled.getCommandType()).isEqualTo("MOVE");
        assertThat(settled.getPayload()).isEqualTo("target=station-3");
        assertThat(settled.getRetryCount()).isZero();
        assertThat(settled.getFinishTime()).isNotNull();
        assertThat(settled.getErrorMessage()).isNull();
    }

    /**
     * 指向不存在设备的指令：留痕成 FAILED，且不阻塞队列。
     *
     * <p>这条盯的是消费者最容易被写坏的地方——把异常抛出去，broker 会 requeue，一条注定失败的消息就能把整条队列卡死。
     * 所以紧接着再投一条正常指令并等它成功：失败的那条没有挡住后面的消息。
     */
    @Test
    void commandForUnknownDeviceIsTracedAsFailedWithoutBlockingTheQueue() {
        String ghostDeviceCode = "it-no-such-device-" + UUID.randomUUID();

        String ghostCommandNo = publish("PICK", ghostDeviceCode, "slot=1");

        DeviceCommandEntity traced = awaitSettled(ghostCommandNo);
        assertThat(traced.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(traced.getDeviceId()).isNull();
        assertThat(traced.getErrorMessage()).contains(ghostDeviceCode);

        createLoopbackDevice();
        String nextCommandNo = publish("PUT", deviceCode, "slot=2");

        assertThat(awaitSettled(nextCommandNo).getStatus()).isEqualTo(CommandStatus.SUCCESS);
    }

    /**
     * 投递用容器里的 {@link RabbitTemplate} 与 {@code zen.ecs.messaging} 配置，测试不另抄一份常量：
     * 抄一份等于把「配置写错」这类失败从断言范围里划出去。
     */
    private String publish(String commandType, String targetDeviceCode, String payload) {
        String commandNo = "CMD-" + UUID.randomUUID();
        issuedCommandNos.add(commandNo);
        rabbitTemplate.convertAndSend(
                properties.getMessaging().getExchange(),
                properties.getMessaging().getRoutingKey(),
                new RcsCommandMessage(commandNo, targetDeviceCode, commandType, payload));
        return commandNo;
    }

    private DeviceResponse createLoopbackDevice() {
        return deviceService.create(new DeviceCreateRequest(
                deviceCode, "集成测试设备", null, LoopbackProtocolSupport.PROTOCOL_TYPE, "loopback://" + deviceCode));
    }

    /** 轮询这条指令直到它离开 PENDING —— 消费者在 broker 线程上，测试只能等它。 */
    private DeviceCommandEntity awaitSettled(String commandNo) {
        long deadline = System.nanoTime() + SETTLE_TIMEOUT.toNanos();
        DeviceCommandEntity command = null;
        while (System.nanoTime() < deadline) {
            command = deviceCommandRepository.findByCommandNo(commandNo).orElse(null);
            if (command != null && command.getStatus() != CommandStatus.PENDING) {
                return command;
            }
            sleepBriefly();
        }
        // 超时不能只报「没等到」：PENDING 与「压根没有行」是两种不同的故障
        throw new AssertionError("等待指令 " + commandNo + " 终态超时（" + SETTLE_TIMEOUT + "），最后可见状态 = "
                + (command == null ? "无记录" : command.getStatus()));
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待指令终态时被中断", e);
        }
    }
}
