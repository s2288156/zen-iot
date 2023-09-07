package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.zeniot.data.enums.NodeTypeEnum;

/**
 * @author Wu.Chunyang
 */
@Data
public class Node implements Cell {

    private String id;

    private Integer x;

    private Integer y;

    private NodeTypeEnum nodeType;

    private String nodeName;

    private JsonNode data;

}
