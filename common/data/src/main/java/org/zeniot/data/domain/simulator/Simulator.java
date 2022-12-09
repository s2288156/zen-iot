package org.zeniot.data.domain.simulator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.zeniot.data.base.DTO;

/**
 * @author Wu.Chunyang
 */
@Data
public class Simulator extends DTO {

    private Long id;

    private String name;

    private JsonNode configInfo;

}
