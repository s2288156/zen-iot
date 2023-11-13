package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zeniot.api.RuleChainService;
import org.zeniot.dao.repository.NodeRelationRepository;
import org.zeniot.dao.repository.NodeRepository;
import org.zeniot.dao.repository.RuleChainRepository;
import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
@Service
public class RuleChainServiceImpl implements RuleChainService {
    @Autowired
    private RuleChainRepository ruleChainRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeRelationRepository nodeRelationRepository;

    @Override
    public RuleChain saveOrUpdateRuleChain(RuleChain ruleChain) {
        final boolean isCreate = ruleChain.getId() == null;
        if (isCreate) {

        }

        return null;
    }
}
