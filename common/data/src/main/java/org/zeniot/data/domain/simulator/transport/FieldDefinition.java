package org.zeniot.data.domain.simulator.transport;

import lombok.Data;
import org.zeniot.data.enums.FieldTypeEnum;

/**
 * @author Wu.Chunyang
 */
@Data
public class FieldDefinition {

    private String name;

    private FieldTypeEnum fieldType;

    /**
     * field value origin
     */
    private String valueOrigin;

    /**
     * field value bound
     */
    private String valueBound;

}
