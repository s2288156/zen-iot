package org.zeniot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.data.domain.rulechain.Edge;
import org.zeniot.data.domain.rulechain.Node;
import org.zeniot.data.domain.rulechain.RuleChain;
import org.zeniot.data.enums.CellShapeEnum;

import static org.zeniot.data.consts.FieldNames.SHAPE_NAME;

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
    void testCellVO2DTO() {
        String graphJson = """
                {"cells":[{"position":{"x":-530,"y":-290},"size":{"width":160,"height":40},"attrs":{"text":{"text":"save timeseries"},"body":{"fill":"#4dd0e1"}},"visible":true,"shape":"RECT_NODE","ports":{"groups":{"top":{"position":"top","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"bottom":{"position":"bottom","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"left":{"position":"left","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"right":{"position":"right","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}}},"items":[{"id":"port_1","group":"top"},{"id":"port_2","group":"bottom"},{"id":"port_3","group":"left"},{"id":"port_4","group":"right"}]},"id":"1a1f3002-4006-4a4b-9748-31e05e4a1050","data":{"nodeType":"SAVE_TIMESERIES"},"tools":{"items":[{"name":"node-editor","args":{"x":-630,"y":13,"attrs":{"backgroundColor":"#EFF4FF"}}}]},"zIndex":1},{"position":{"x":-182,"y":-290},"size":{"width":160,"height":40},"attrs":{"text":{"text":"save attributes"},"body":{"fill":"#e57373"}},"visible":true,"shape":"RECT_NODE","ports":{"groups":{"top":{"position":"top","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"bottom":{"position":"bottom","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"left":{"position":"left","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}},"right":{"position":"right","attrs":{"circle":{"magnet":true,"stroke":"#8f8f8f","r":3,"style":{"visibility":"hidden"}}}}},"items":[{"id":"port_1","group":"top"},{"id":"port_2","group":"bottom"},{"id":"port_3","group":"left"},{"id":"port_4","group":"right"}]},"id":"49cfd9ef-ce32-4a9b-bc5f-6d71edfbd4ad","data":{"nodeType":"SAVE_ATTRIBUTES"},"tools":{"items":[{"name":"node-editor","args":{"x":-630,"y":13,"attrs":{"backgroundColor":"#EFF4FF"}}}]},"zIndex":2},{"shape":"edge","id":"405c76c4-9739-404e-96aa-a9194d4cb5d2","source":{"cell":"1a1f3002-4006-4a4b-9748-31e05e4a1050","port":"port_4"},"target":{"cell":"49cfd9ef-ce32-4a9b-bc5f-6d71edfbd4ad","port":"port_3"},"zIndex":3}]}
                """;
        RuleChain ruleChain = JacksonUtil.readValue(graphJson, RuleChain.class);
        ruleChain.getCells().forEach(cell -> {
            if (StringUtils.equalsIgnoreCase(cell.get(SHAPE_NAME).asText(), CellShapeEnum.EDGE.name())) {
                Edge edge = Edge.fromVo(cell);
                log.warn("{}", edge);
            } else if (StringUtils.equalsIgnoreCase(cell.get(SHAPE_NAME).asText(), CellShapeEnum.RECT_NODE.name())) {
                Node node = Node.fromVo(cell);
                log.warn("{}", node);
            }
        });
    }
}
