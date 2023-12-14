package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.zeniot.dao.model.RuleChainEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class RuleChainRepositoryTest {
    @Autowired
    private RuleChainRepository ruleChainRepository;
    @Test
    void test_query_rule_chains() {
        RuleChainEntity ruleChainEntity = ruleChainRepository.findById(921460662146830336L).orElseThrow();
        assertNotNull(ruleChainEntity);
        log.info("{}", ruleChainEntity);
    }
}
