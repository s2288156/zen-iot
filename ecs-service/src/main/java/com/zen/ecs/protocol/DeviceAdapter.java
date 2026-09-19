package com.zen.ecs.protocol;

/**
 * 设备协议适配器：连接 / 读 / 写 / 状态。ECS 与外部设备之间的一切字节读写都收敛到这四个动作上。
 *
 * <p>实例是**有状态**的：一个适配器对应一条到具体设备的连接，不是共享单例，因此不标 {@code @Component}，由
 * {@link DeviceAdapterFactory} 按次创建、调用方负责 {@link #close()}。Phase 4 的 Modbus / OPC UA
 * 适配器会在此基础上做连接复用，本阶段不做（避免为一个假协议先建一层连接池）。
 *
 * <p>实现必须能容忍被中断：{@code com.zen.ecs.service.CommandDispatchService} 在等待超时时会中断执行线程，
 * 阻塞在 socket 上的实现要能因此退出。
 */
public interface DeviceAdapter extends AutoCloseable {

    /** 建立到设备的连接；失败抛运行时异常，由指令中转按可重试失败处理。 */
    void connect();

    /** 当前连接状态，用于排查与 Phase 4 的状态推送。 */
    AdapterStatus status();

    /** 读一次设备状态量（线圈/寄存器/节点值），语义由具体协议定义。 */
    String read();

    /** 写一条指令；{@code accepted=false} 表示设备明确拒绝。 */
    CommandOutcome write(String commandType, String payload);

    @Override
    void close();
}
