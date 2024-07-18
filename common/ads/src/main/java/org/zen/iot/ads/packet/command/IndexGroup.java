package org.zen.iot.ads.packet.command;

import org.zen.iot.common.util.DataTypeConvertor;

/**
 * @author Jack Wu
 */
public enum IndexGroup {
    /**
     * PLC memory range(%M field).Offset is byte offset.
     */
    READ_WRITE_M("00004020"),
    /**
     * PLC memory range (%MX field).The low word of the index offset is the byte offset.
     * The index offset contains the bit address calculated from the byte number *8 + bit number
     */
    READ_WRITE_MX("00004021"),
    /**
     * Byte length of the process diagram of the memory range
     */
    PLC_ADS_IGR_RM_SIZE("00004025"),
    /**
     * Retain data range. The index offset is byte offset
     */
    PLC_ADS_IGR_RW_RB("00004030"),
    /**
     * Byte length of the retain range
     */
    PLC_ADS_IGR_RR_SIZE("00004035"),
    /**
     * Data range. The index offset is byte offset.
     */
    PLC_ADS_IGR_RW_DB("00004040"),
    /**
     * Byte length of the data range
     */
    PLC_ADS_IGR_RD_SIZE("00004045"),
    ;
    private final String id;

    public static final Integer length = 4;

    IndexGroup(String id) {
        this.id = id;
    }

    public byte[] toLittleEndianBytes() {
        return DataTypeConvertor.hexToBytes(this.id);
    }

}
