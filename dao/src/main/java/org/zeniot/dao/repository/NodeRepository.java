package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.NodeEntity;

public interface NodeRepository extends JpaRepository<NodeEntity, Long> {
    void deleteByRuleChainId(Long ruleChainId);
}
