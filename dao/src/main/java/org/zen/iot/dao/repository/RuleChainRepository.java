package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.RuleChainEntity;

public interface RuleChainRepository extends JpaRepository<RuleChainEntity, Long> {
}
