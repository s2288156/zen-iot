package org.zen.iot.data.domain.simulator.transport;

import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.zen.iot.data.enums.FieldTypeEnum;

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

    public String nextRandomValue() {
        Object randomValue = null;
        switch (fieldType) {
            case INTEGER -> randomValue = RandomUtils.nextInt(Integer.parseInt(valueOrigin), Integer.parseInt(valueBound));
            case DOUBLE -> randomValue = RandomUtils.nextDouble(Double.parseDouble(valueOrigin), Double.parseDouble(valueBound));
        }
        return String.valueOf(randomValue);
    }

}
