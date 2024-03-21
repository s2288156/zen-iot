package org.zeniot.data.domain.rulechain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.zeniot.data.base.DTO;
import org.zeniot.data.base.HasId;

import java.time.LocalDateTime;

/**
 * @author Wu.Chunyang
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class Edge extends DTO implements HasId<String> {

    private String id;

    private String sourceId;

    private String sourcePort;

    private String targetId;

    private String targetPort;

    private Long ruleChainId;

}
