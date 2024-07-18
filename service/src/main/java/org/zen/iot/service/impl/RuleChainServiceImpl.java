package org.zen.iot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zen.iot.api.RuleChainService;
import org.zen.iot.dao.model.NodeEntity;
import org.zen.iot.dao.model.NodeRelationEntity;
import org.zen.iot.dao.model.RuleChainEntity;
import org.zen.iot.dao.repository.NodeRelationRepository;
import org.zen.iot.dao.repository.NodeRepository;
import org.zen.iot.dao.repository.RuleChainRepository;
import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.rulechain.RuleChain;
import org.zen.iot.service.mapper.RuleChainMapper;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Slf4j
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
    public RuleChain createOrUpdateRuleChain(RuleChain ruleChain) {
        final boolean isCreate = ruleChain.getId() == null;
        RuleChainEntity ruleChainEntity;
        ruleChainEntity = ruleChainMapper.toEntity(ruleChain);
        if (!isCreate) {
            // TODO: 2/18/2024 update logic
            nodeRepository.deleteByRuleChainId(ruleChain.getId());
            nodeRelationRepository.deleteByRuleChainId(ruleChain.getId());
        }
        ruleChainRepository.save(ruleChainEntity);
        List<NodeEntity> nodeEntities = ruleChain.getNodes().stream()
                .map(node -> ruleChainMapper.toEntity(node, ruleChainEntity.getId()))
                .toList();
        nodeRepository.saveAll(nodeEntities);
        List<NodeRelationEntity> nodeRelationEntities = ruleChain.getEdges().stream()
                .map(edge -> ruleChainMapper.toEntity(edge, ruleChainEntity.getId()))
                .toList();
        nodeRelationRepository.saveAll(nodeRelationEntities);
        ruleChain.setId(ruleChainEntity.getId());
        return ruleChain;
    }

    @Transactional
    @Override
    public PageResponse<RuleChain> findRuleChains(PageQuery pageQuery) {
        Page<RuleChainEntity> ruleChainPage = ruleChainRepository.findAll(pageQuery.toPageable());
        List<RuleChain> ruleChains = ruleChainPage.getContent()
                .stream()
                .map(ruleChainMapper::entityToRuleChain)
                .toList();
        return PageResponse.success(ruleChains, ruleChainPage);
    }

    @Transactional
    @Override
    public RuleChain getRuleChain(Long id) {
        RuleChainEntity ruleChainEntity = ruleChainRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("RuleChain not found"));
        return ruleChainMapper.entityToRuleChain(ruleChainEntity);
    }

    @Override
    public void deleteRuleChain(Long id) {
        ruleChainRepository.deleteById(id);
    }
}
