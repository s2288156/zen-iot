package org.zen.iot.data.domain.simulator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.zen.iot.data.base.DTO;
import org.zen.iot.data.domain.simulator.transport.SimulatorTransportConfig;
import org.zen.iot.data.enums.SimulatorStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

import java.time.LocalDateTime;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Simulator extends DTO {

    private Long id;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "Transport Type不能为空")
    private TransportTypeEnum transportType;

    private SimulatorTransportConfig transportConfig;

    @Setter
    private SimulatorStatusEnum status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
