package com.zen.admin;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    /** 分层归属由类名即可判定:名字带角色后缀却落在别的包,上面那几条分层规则会直接漏判。 */
    @ParameterizedTest
    @CsvSource({
        "Controller, controller",
        "Service, service",
        "Repository, repository",
        "Entity, entity",
    })
    void roleSuffixResidesInItsOwnPackage(String suffix, String layerPackage) {
        classes()
                .that()
                .haveSimpleNameEndingWith(suffix)
                .should()
                .resideInAPackage("com.zen.admin." + layerPackage + "..")
                .check(ADMIN_SERVICE);
    }

    /** 当前全是构造器注入,固化现状:字段注入让依赖隐形、无法声明 final、脱离 Spring 容器就构造不出可用对象。 */
    @Test
    void neverInjectsByField() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(ADMIN_SERVICE);
    }

    /**
     * 输出统一走日志框架:P3-1 已把 traceId 打进日志,而标准流输出既拿不到 traceId、也不受日志级别控制。
     * ArchUnit 的这条内置规则同时覆盖 {@code System.out}/{@code System.err} 与 {@code Throwable#printStackTrace}。
     */
    @Test
    void neverWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(ADMIN_SERVICE);
    }

    /**
     * 时间源统一:{@code new Date()} 隐式读系统时钟、无法注入测试时钟。
     * 刻意只禁构造调用而不禁整个 {@code java.util.Date}——jjwt 的 {@code issuedAt}/{@code expiration} 签名只收 Date。
     */
    @Test
    void neverConstructsJavaUtilDate() {
        noClasses()
                .should()
                .callConstructor("java.util.Date")
                .because("用 java.time;java.util.Date 只允许作为 jjwt 的 API 边界短暂出现")
                .check(ADMIN_SERVICE);
    }

    /** 包切片之间无循环依赖:成环会让任一侧改动牵连另一侧,也让后续按包拆分服务变成不可能。 */
    @Test
    void packageSlicesAreFreeOfCycles() {
        slices().matching("com.zen.admin.(*)..").should().beFreeOfCycles().check(ADMIN_SERVICE);
    }
}
