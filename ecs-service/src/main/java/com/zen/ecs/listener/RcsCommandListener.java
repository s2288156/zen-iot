package com.zen.ecs.listener;

import com.zen.ecs.dto.RcsCommandMessage;
import com.zen.ecs.service.CommandDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RCS → ECS 指令通道的消费者，只做「收下消息交给中转」，不含任何业务判断。
 *
 * <p>类名不以 {@code Service} 结尾、也不标 {@code @Transactional}：事务边界在
 * {@link CommandDispatchService} 的落库动作上（中转含设备 I/O，不能长时间占住数据库连接）。
 */
@Slf4j
@Component
public class RcsCommandListener {

    private final CommandDispatchService commandDispatchService;

    public RcsCommandListener(CommandDispatchService commandDispatchService) {
        this.commandDispatchService = commandDispatchService;
    }

    /**
     * 异常一律在此吞掉：重新抛出会让 broker 把消息 requeue，一条注定失败的消息（比如指向不存在设备的指令）
     * 会把消费者卡在无限重投里。留痕与告警由中转和日志负责。
     */
    @RabbitListener(queues = "${zen.ecs.messaging.queue}")
    public void onCommand(RcsCommandMessage message) {
        try {
            commandDispatchService.dispatch(message);
        } catch (RuntimeException e) {
            log.error("指令消费失败，已丢弃消息（不重投）", e);
        }
    }
}
