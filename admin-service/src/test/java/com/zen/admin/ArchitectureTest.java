package com.zen.admin;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** 后台服务的分层与边界约束:controller → service → repository → entity 单向,禁止反向依赖。 */
@Tag("architecture")
class ArchitectureTest {

    private static final JavaClasses ADMIN_SERVICE = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.zen.admin");

    @Test
    void layersOnlyDependDownwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller")
                .definedBy("com.zen.admin.controller..")
                .layer("Service")
                .definedBy("com.zen.admin.service..")
                .layer("Repository")
                .definedBy("com.zen.admin.repository..")
                .layer("Entity")
                .definedBy("com.zen.admin.entity..")
                .whereLayer("Controller")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository")
                .mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Entity")
                .mayOnlyBeAccessedByLayers("Service", "Repository")
                .check(ADMIN_SERVICE);
    }

    /** 对外契约一律走 DTO,{@code jakarta.persistence} 实体不得越过 service/repository 出现在入口层。 */
    @Test
    void controllersNeverTouchPersistenceEntities() {
        noClasses()
                .that()
                .resideInAPackage("com.zen.admin.controller..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.zen.admin.entity..")
                .because("实体不出入口层,避免持久化结构泄漏成对外接口契约")
                .check(ADMIN_SERVICE);
    }

    /** 事务边界只放在 service 层:controller 开事务会拉长连接占用,repository 开事务会撕裂业务原子性。 */
    @Test
    void transactionalOnlyAppearsInServiceLayer() {
        noClasses()
                .that()
                .resideOutsideOfPackage("com.zen.admin.service..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.transaction.annotation.Transactional")
                .because("@Transactional 只允许出现在 com.zen.admin.service 层")
                .check(ADMIN_SERVICE);
    }
}
