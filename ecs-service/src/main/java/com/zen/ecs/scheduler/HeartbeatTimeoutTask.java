package com.zen.ecs.scheduler;

import com.zen.ecs.service.HeartbeatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 心跳超时探测。
 *
 * <p>类名不以 {@code Service} 结尾、也不标 {@code @Transactional}：{@code roleSuffixResidesInItsOwnPackage} 会要求
 * {@code *Service} 住在 service 包，而事务边界归 {@link HeartbeatService}。任务只做「按周期触发 + 记日志」。
 *
 * <p>{@code fixedDelay} 是上一次执行结束到下一次开始，探测本身要扫在线设备并写事件，慢一轮不会让下一轮叠上来。
 */
@Slf4j
@Component
public class HeartbeatTimeoutTask {

    private final HeartbeatService heartbeatService;

    public HeartbeatTimeoutTask(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @Scheduled(fixedDelayString = "${zen.ecs.heartbeat.probe-interval}")
    public void probe() {
        int offline = heartbeatService.detectTimeouts();
        if (offline > 0) {
            log.info("心跳超时探测完成：{} 台设备置为离线", offline);
        }
    }
}
