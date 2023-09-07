package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import static org.zeniot.data.consts.FieldNames.*;

/**
 * @author Wu.Chunyang
 */
@Data
public class Edge implements Cell{

    private String id;

    private String sourceId;

    private String sourcePort;

    private String targetId;

    private String targetPort;

    public static Edge fromVo(JsonNode cell) {
        Edge edge = new Edge();
        edge.setId(cell.get(ID_NAME).asText());
        edge.setSourceId(cell.get(SOURCE_NAME).get(CELL_NAME).asText());
        edge.setSourcePort(cell.get(SOURCE_NAME).get(PORT_NAME).asText());
        edge.setTargetId(cell.get(TARGET_NAME).get(CELL_NAME).asText());
        edge.setTargetPort(cell.get(TARGET_NAME).get(PORT_NAME).asText());
        return edge;
    }
}
