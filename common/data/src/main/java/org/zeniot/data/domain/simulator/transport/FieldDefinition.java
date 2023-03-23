package org.zeniot.data.domain.simulator.transport;

import org.zeniot.data.enums.FieldTypeEnum;

/**
 * @author Wu.Chunyang
 */
public class FieldDefinition {

    private String name;

    private FieldTypeEnum fieldType;

    /**
     * field value origin
     */
    private String origin;

    /**
     * field value bound
     */
    private String bound;

}
