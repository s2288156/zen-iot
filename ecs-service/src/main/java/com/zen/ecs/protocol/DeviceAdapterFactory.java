package com.zen.ecs.protocol;

import com.zen.common.security.error.BusinessException;
import com.zen.ecs.error.EcsErrorCode;
import java.util.List;

/** 按协议类型挑选适配器实现。 */
public class DeviceAdapterFactory {

    private final List<ProtocolSupport> supports;

    /**
     * 入参由 Spring 收集全部 {@link ProtocolSupport} Bean 注入，构造期做一次 {@link List#copyOf} 固化：
     * 既避免调用方事后修改列表影响选路（也就不用给 EI_EXPOSE_REP2 开豁免），也让「没有适配器」在启动后是稳定事实。
     */
    public DeviceAdapterFactory(List<ProtocolSupport> supports) {
        this.supports = List.copyOf(supports);
    }

    /** 设备档案里的协议类型无人认领时抛业务异常，而不是静默丢指令。 */
    public DeviceAdapter open(DeviceTarget target) {
        return supports.stream()
                .filter(support -> support.supports(target.protocolType()))
                .findFirst()
                .map(support -> support.open(target))
                .orElseThrow(() -> new BusinessException(
                        EcsErrorCode.PROTOCOL_NOT_SUPPORTED,
                        "协议类型 " + target.protocolType() + " 没有可用适配器（设备 " + target.deviceCode() + "）"));
    }
}
