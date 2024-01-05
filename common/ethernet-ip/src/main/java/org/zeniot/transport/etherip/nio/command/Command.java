package org.zeniot.transport.etherip.nio.command;

import lombok.Getter;

/**
 * @author Jack Wu
 */
@Getter
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

    private final short code;

    Command(int code) {
        this.code = (short) code;
    }

    public static Command forCode(final short code)
    {
        for (final Command command : values())
        {
            if (command.code == code)
            {
                return command;
            }
        }
        return null;
    }
}
