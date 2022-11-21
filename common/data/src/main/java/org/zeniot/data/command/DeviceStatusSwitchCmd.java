package org.zeniot.data.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.base.Cmd;
import org.zeniot.data.base.HasId;
import org.zeniot.data.enums.DeviceStatusEnum;

import javax.validation.constraints.NotNull;

/**
 * @author Wu.Chunyang
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceStatusSwitchCmd extends Cmd implements HasId {

    @NotNull
    private Long id;

    @NotNull
    private DeviceStatusEnum deviceStatus;

}
