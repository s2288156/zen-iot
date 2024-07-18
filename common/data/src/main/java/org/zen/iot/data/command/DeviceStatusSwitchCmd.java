package org.zen.iot.data.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zen.iot.data.base.Cmd;
import org.zen.iot.data.base.HasId;
import org.zen.iot.data.enums.DeviceStatusEnum;


/**
 * @author Wu.Chunyang
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceStatusSwitchCmd extends Cmd implements HasId<Long> {

    @NotNull
    private Long id;

    @NotNull
    private DeviceStatusEnum deviceStatus;

}
