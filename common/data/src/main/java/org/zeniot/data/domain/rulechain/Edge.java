package org.zeniot.data.domain.rulechain;

import lombok.Data;

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
}
