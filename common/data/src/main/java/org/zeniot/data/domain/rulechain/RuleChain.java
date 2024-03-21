package org.zeniot.data.domain.rulechain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RuleChain extends DTO implements HasId<Long> {

    private Long id;

    @NotBlank(message = "The rule chain name cannot empty.")
    private String name;

    private List<Node> nodes;

    private List<Edge> edges;

    private LocalDateTime updateTime;

    private JsonNode graphJson;

    private LocalDateTime createTime;
}
