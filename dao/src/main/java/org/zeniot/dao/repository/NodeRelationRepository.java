package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.NodeRelationEntity;

public interface NodeRelationRepository extends JpaRepository<NodeRelationEntity, Long> {
    void deleteByRuleChainId(Long ruleChainId);
}
