package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.NodeRelationEntity;

public interface NodeRelationRepository extends JpaRepository<NodeRelationEntity, Long> {
    void deleteByRuleChainId(Long ruleChainId);
}
