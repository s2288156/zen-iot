package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.zeniot.data.enums.NodeTypeEnum;

import static org.zeniot.data.consts.FieldNames.*;

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

    public static Node fromVo(JsonNode cell) {
        Node node = new Node();
        node.setId(cell.get(ID_NAME).asText());
        node.setX(cell.get(POSITION_NAME).get(X_NAME).asInt());
        node.setY(cell.get(POSITION_NAME).get(Y_NAME).asInt());
        node.setNodeType(NodeTypeEnum.valueOf(cell.get(DATA_NAME).get(NODE_TYPE_NAME).asText()));
        node.setNodeName(cell.get(ATTRS_NAME).get(TEXT_NAME).get(TEXT_NAME).asText());
        return node;
    }
}
