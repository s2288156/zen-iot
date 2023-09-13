package org.zeniot.data.vo.graph;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;
import org.zeniot.data.enums.NodeTypeEnum;

/**
 * @author Wu.Chunyang
 */
public class NodeVO extends DTO implements HasId<String> {
    @Setter
    @Getter
    private String id;
    private JsonNode positionInfo;
    private String nodeName;
    private String shape;
    private NodeTypeEnum nodeType;
    private JsonNode configData;
}
