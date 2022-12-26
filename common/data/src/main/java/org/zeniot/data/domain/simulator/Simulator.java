package org.zeniot.data.domain.simulator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.zeniot.data.base.DTO;
import org.zeniot.data.domain.transport.TransportConfig;
import org.zeniot.data.enums.SimulatorStatusEnum;
import org.zeniot.data.enums.TransportTypeEnum;

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

    private TransportConfig transportConfig;

    @Setter
    private SimulatorStatusEnum status;

}