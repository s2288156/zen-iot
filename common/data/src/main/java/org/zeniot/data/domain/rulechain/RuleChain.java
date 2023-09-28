package org.zeniot.data.domain.rulechain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RuleChain extends DTO implements HasId<Long> {

    private Long id;

    private List<Node> nodes;

    private List<Edge> edges;

}
