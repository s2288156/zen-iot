package org.zen.iot.data.enums;

public enum SimulatorStatusEnum {
    DISABLE, ENABLE,
    ;

    public SimulatorStatusEnum reverse() {
        if (this == DISABLE) {
            return ENABLE;
        } else {
            return DISABLE;
        }
    }
}
