package org.zen.iot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.zen.iot.data.base.DTO;
import org.zen.iot.data.base.HasId;
import org.zen.iot.data.enums.NodeTypeEnum;

/**
 * @author Wu.Chunyang
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class Node extends DTO implements HasId<String> {
    private String id;
    private String nodeName;
    private NodeTypeEnum nodeType;
    private JsonNode metadata;
    private Long ruleChainId;
}
