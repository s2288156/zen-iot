package org.zeniot.data.domain.rulechain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Edge extends DTO implements HasId<String> {

    private String id;

    private String sourceId;

    private String sourcePort;

    private String targetId;

    private String targetPort;
}
