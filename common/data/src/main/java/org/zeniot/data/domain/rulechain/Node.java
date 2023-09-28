package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;
import org.zeniot.data.enums.NodeTypeEnum;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Node extends DTO implements HasId<String> {
    private String id;
    private JsonNode positionInfo;
    private String nodeName;
    private String shape;
    private NodeTypeEnum nodeType;
    private JsonNode configData;
}
