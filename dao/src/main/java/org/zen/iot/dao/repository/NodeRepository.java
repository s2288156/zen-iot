package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.NodeEntity;

public interface NodeRepository extends JpaRepository<NodeEntity, Long> {
    void deleteByRuleChainId(Long ruleChainId);
}
