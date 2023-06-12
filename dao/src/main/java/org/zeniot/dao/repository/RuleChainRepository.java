package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.RuleChainEntity;

public interface RuleChainRepository extends JpaRepository<RuleChainEntity, Long> {
}
