package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;
import org.zeniot.data.enums.NodeTypeEnum;

import java.time.LocalDateTime;

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
