package org.zeniot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.data.domain.rulechain.Edge;
import org.zeniot.data.domain.rulechain.RuleChain;
import org.zeniot.data.enums.CellShapeEnum;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class DemoTest {

    @Test
    void testBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String inputPwd = "123123";
//        String encodePwd = encoder.encode(inputPwd);
        String encodePwd = "$2a$10$192rKbVwo2TGc0mOw//.AeDZdBmVZappJ.ywohE2L/pCH4eSTPdEa";
        log.info("inputPwd: {}, encodePwd: {}", inputPwd, encodePwd);
        log.warn("{}", encoder.matches(inputPwd, encodePwd));
    }

    @Test
    void testGraphParse() {
        String graphJson = """
                {"cells":[{"position":{"x":-490,"y":-250},"size":{"width":160,"height":40},"attrs":{"text":{"text":"save timeseries"},"body":{"fill":"#4dd0e1"}},"visible":true,"shape":"RECT_NODE","ports":{"groups":{"top":{"position":"top","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"bottom":{"position":"bottom","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"left":{"position":"left","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"right":{"position":"right","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}}},"items":[{"id":"port_1","group":"top"},{"id":"port_2","group":"bottom"},{"id":"port_3","group":"left"},{"id":"port_4","group":"right"}]},"id":"edbc71fa-6b21-4abb-8208-12efd9b93186","data":{},"tools":{"items":[{"name":"node-editor","args":{"x":-630,"y":13,"attrs":{"backgroundColor":"#EFF4FF"}}}]},"zIndex":1},{"position":{"x":-138,"y":-250},"size":{"width":160,"height":40},"attrs":{"text":{"text":"save attributes"},"body":{"fill":"#e57373"}},"visible":true,"shape":"RECT_NODE","ports":{"groups":{"top":{"position":"top","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"bottom":{"position":"bottom","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"left":{"position":"left","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"right":{"position":"right","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}}},"items":[{"id":"port_1","group":"top"},{"id":"port_2","group":"bottom"},{"id":"port_3","group":"left"},{"id":"port_4","group":"right"}]},"id":"df422d41-7a93-4e74-9e47-415a452c6d17","data":{},"tools":{"items":[{"name":"node-editor","args":{"x":-630,"y":13,"attrs":{"backgroundColor":"#EFF4FF"}}}]},"zIndex":2},{"shape":"edge","id":"ded93b05-ea6e-4665-8ad6-a4dacc55a21b","source":{"cell":"edbc71fa-6b21-4abb-8208-12efd9b93186","port":"port_4"},"target":{"cell":"df422d41-7a93-4e74-9e47-415a452c6d17","port":"port_3"},"zIndex":3}]}
                """;
        RuleChain ruleChain = JacksonUtil.readValue(graphJson, RuleChain.class);
        ruleChain.getCells().forEach(cell -> {
            if (StringUtils.equalsIgnoreCase(cell.get("shape").asText(), CellShapeEnum.EDGE.name())) {
                Edge edge = Edge.fromVo(cell);
                log.warn("{}", edge);
            } else if (StringUtils.equalsIgnoreCase(cell.get("shape").asText(), CellShapeEnum.RECT_NODE.name())) {
                log.warn("RECT_NODE");
            }
        });
    }
}
