package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeniot.api.RuleChainService;
import org.zeniot.dao.model.NodeEntity;
import org.zeniot.dao.model.NodeRelationEntity;
import org.zeniot.dao.model.RuleChainEntity;
import org.zeniot.dao.repository.NodeRelationRepository;
import org.zeniot.dao.repository.NodeRepository;
import org.zeniot.dao.repository.RuleChainRepository;
import org.zeniot.data.domain.rulechain.RuleChain;
import org.zeniot.service.mapper.RuleChainMapper;

import java.util.List;

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

    private final RuleChainMapper ruleChainMapper = RuleChainMapper.INSTANCE;

    @Transactional
    @Override
    public RuleChain saveOrUpdateRuleChain(RuleChain ruleChain) {
        final boolean isCreate = ruleChain.getId() == null;
        RuleChainEntity ruleChainEntity;
        if (isCreate) {
            ruleChainEntity = ruleChainMapper.toEntity(ruleChain);
            ruleChainRepository.save(ruleChainEntity);
            List<NodeEntity> nodeEntities = ruleChain.getNodes().stream()
                    .map(node -> ruleChainMapper.toEntity(node, ruleChainEntity.getId()))
                    .toList();
            nodeRepository.saveAll(nodeEntities);
            List<NodeRelationEntity> nodeRelationEntities = ruleChain.getEdges().stream()
                    .map(ruleChainMapper::toEntity)
                    .toList();
            nodeRelationRepository.saveAll(nodeRelationEntities);
            ruleChain.setId(ruleChainEntity.getId());
        }

        return ruleChain;
    }
}
