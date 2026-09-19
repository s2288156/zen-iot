package com.zen.ecs.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** ECS 侧可调项，前缀 {@code zen.ecs}。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zen.ecs")
public class ZenEcsProperties {

    private final Heartbeat heartbeat = new Heartbeat();

    private final Command command = new Command();

    private final Messaging messaging = new Messaging();

    /** 心跳超时判定：{@code timeout} 是「多久没心跳算离线」，{@code probeInterval} 是探测周期。 */
    @Getter
    @Setter
    public static class Heartbeat {
        private Duration timeout = Duration.ofSeconds(30);
        private Duration probeInterval = Duration.ofSeconds(10);
    }

    /** 指令中转：单次等待上限 {@code timeout}，总尝试次数 {@code maxAttempts}（含首次，故 1 表示不重试）。 */
    @Getter
    @Setter
    public static class Command {
        private Duration timeout = Duration.ofSeconds(5);
        private int maxAttempts = 3;
    }

    /** RCS → ECS 指令通道的名字。完整拓扑与命名规范随 Phase 9 定案。 */
    @Getter
    @Setter
    public static class Messaging {
        private String exchange = "zen.rcs.command";
        private String queue = "ecs.rcs.command";
        private String routingKey = "rcs.command.ecs";
    }
}
