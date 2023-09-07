package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Data
public class RuleChain {

    // private List<Cell> cells;
    //
    // private List<Edge> edges;

    private List<JsonNode> cells;

}
