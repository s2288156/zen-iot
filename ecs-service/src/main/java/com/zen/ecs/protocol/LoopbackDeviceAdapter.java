package com.zen.ecs.protocol;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 回环适配器：把写进去的指令原样读回来，不碰任何网络。
 *
 * <p>它是协议插槽的参照实现：让「设备档案 → MQ 消费 → 适配器执行」这条链路在还没有真实设备的 Phase 3 就能被跑通和测通。
 * Phase 4 的 Modbus / OPC UA 实现照这一份的形态替换即可。
 */
public class LoopbackDeviceAdapter implements DeviceAdapter {

    private static final String IDLE = "IDLE";

    private final String deviceCode;
    private final String endpoint;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<String> lastWritten = new AtomicReference<>(IDLE);

    public LoopbackDeviceAdapter(String deviceCode, String endpoint) {
        this.deviceCode = deviceCode;
        this.endpoint = endpoint;
    }

    @Override
    public void connect() {
        connected.set(true);
    }

    @Override
    public AdapterStatus status() {
        return new AdapterStatus(
                connected.get(), "loopback://" + deviceCode + (endpoint == null ? "" : "?" + endpoint));
    }

    @Override
    public String read() {
        if (!connected.get()) {
            throw new IllegalStateException("回环适配器未连接: " + deviceCode);
        }
        return lastWritten.get();
    }

    @Override
    public CommandOutcome write(String commandType, String payload) {
        if (!connected.get()) {
            throw new IllegalStateException("回环适配器未连接: " + deviceCode);
        }
        String echoed = commandType + "=" + (payload == null ? "" : payload);
        lastWritten.set(echoed);
        return CommandOutcome.accepted(echoed);
    }

    @Override
    public void close() {
        connected.set(false);
    }
}
