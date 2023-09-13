package org.zeniot.data.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.base.Cmd;
import org.zeniot.data.base.HasId;
import org.zeniot.data.enums.SimulatorStatusEnum;


/**
 * @author Wu.Chunyang
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SimulatorSwitchPowerCmd extends Cmd implements HasId<Long> {

    @NotNull
    private Long id;

    @NotNull
    private SimulatorStatusEnum status;

}
