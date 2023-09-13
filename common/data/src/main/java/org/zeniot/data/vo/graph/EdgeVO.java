package org.zeniot.data.vo.graph;

import lombok.Getter;
import lombok.Setter;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;

/**
 * @author Wu.Chunyang
 */
public class EdgeVO extends DTO implements HasId<String> {

    @Setter
    @Getter
    private String id;

    private String sourceId;

    private String sourcePort;

    private String targetId;

    private String targetPort;
}
