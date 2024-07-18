package org.zen.iot.transport.etherip.nio.protocol;


import org.zen.iot.transport.etherip.nio.types.CNPath;
import org.zen.iot.transport.etherip.nio.types.CNService;


/**
 * Message Router protocol for writing a tag
 *
 * @author Jack Wu
 */
public class MRChipWriteProtocol extends MessageRouterProtocol {
    /**
     * Initialize
     *
     * @param tag   Name of tag to write
     * @param value {@link CIPData} to write
     */
    public MRChipWriteProtocol(final String tag, final CIPData value) {
        super(CNService.CIP_WriteData, CNPath.Symbol(tag),
                new CIPWriteDataProtocol(value));
    }
}
