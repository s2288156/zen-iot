package org.zen.iot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zen.iot.api.DeviceService;
import org.zen.iot.api.RuleChainService;
import org.zen.iot.common.util.JacksonUtil;
import org.zen.iot.dao.repository.DeviceRepository;
import org.zen.iot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
@ActiveProfiles(profiles = "test")
@SpringBootTest
public class AbstractBootTest {
    @Autowired
    public DeviceService deviceService;
    @Autowired
    public RuleChainService ruleChainService;
    @Autowired
    public DeviceRepository deviceRepository;

    public static final RuleChain ruleChain1 = JacksonUtil.readValue("{\"name\":\"1\",\"nodes\":[{\"id\":\"e1aae1a2-8b4a-417b-84e2-8ad0d1c84d23\",\"nodeName\":\"save timeseries\",\"nodeType\":\"SAVE_TIMESERIES\",\"metadata\":{\"position\":{\"x\":-430,\"y\":-310},\"shape\":\"rect_node\",\"data\":{\"nodeType\":\"SAVE_TIMESERIES\"}}},{\"id\":\"2a93e471-a924-4045-85a1-b53808adf243\",\"nodeName\":\"save attributes\",\"nodeType\":\"SAVE_ATTRIBUTES\",\"metadata\":{\"position\":{\"x\":-70,\"y\":-320},\"shape\":\"rect_node\",\"data\":{\"nodeType\":\"SAVE_ATTRIBUTES\"}}}],\"edges\":[{\"id\":\"5291ccc6-7990-4894-bece-e674645f2a94\",\"sourceId\":\"e1aae1a2-8b4a-417b-84e2-8ad0d1c84d23\",\"sourcePort\":\"port_4\",\"targetId\":\"2a93e471-a924-4045-85a1-b53808adf243\",\"targetPort\":\"port_3\"}]}", RuleChain.class);
}
