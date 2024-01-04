package org.zeniot.transport.etherip;

import lombok.Getter;

/**
 * @author Jack Wu
 */
public enum Command {
    Nop(0x0000),
    ListServices(0x0004),
    ListIdentity(0x0063),
    ListInterfaces(0x0064),
    RegisterSession(0x0065),
    UnRegisterSession(0x0066),
    SendRRData(0x006F),
    SendUnitData(0x0070),
    IndicateStatus(0x0072),
    Cancel(0x0073);

    @Getter
    private final short code;

    Command(int code) {
        this.code = (short) code;
    }
}
