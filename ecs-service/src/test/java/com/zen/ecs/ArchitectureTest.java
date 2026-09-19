package com.zen.ecs;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** ECS 服务的分层与边界约束：controller → service → repository → entity 单向，协议适配与心跳判定的时间源必须可注入。 */
@Tag("architecture")
class ArchitectureTest {

    private static final JavaClasses ECS_SERVICE = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.zen.ecs");

    /**
     * 反「静默失效」哨兵，挡的是**部分**失配：某个子包被改名或挪出 {@code com.zen.ecs} 后，剩下的类仍足以让下面每条规则
     * 判定通过，门禁看着全绿，覆盖面却已经缩水。整包导空是另一种失效——实测本仓库的 ArchUnit 版本对「一条类都没检查」
     * 的规则直接判红（14/14 全红），所以那条已由规则自己拦住，这里不重复依赖它。
     *
     * <p>阈值取接入时实测 48 类的下沿：类数随功能增长，只会偶发下降，降到阈值以下就说明有整包不见了。
     */
    @Test
    void importedClassesAreNeverEmpty() {
        assertThat(ECS_SERVICE.size()).isGreaterThan(40);
    }

    @Test
    void layersOnlyDependDownwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller")
                .definedBy("com.zen.ecs.controller..")
                .layer("Service")
                .definedBy("com.zen.ecs.service..")
                .layer("Repository")
                .definedBy("com.zen.ecs.repository..")
                .layer("Entity")
                .definedBy("com.zen.ecs.entity..")
                .whereLayer("Controller")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository")
                .mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Entity")
                .mayOnlyBeAccessedByLayers("Service", "Repository")
                .check(ECS_SERVICE);
    }

    /** 对外契约一律走 DTO，{@code jakarta.persistence} 实体不得越过 service/repository 出现在入口层。 */
    @Test
    void controllersNeverTouchPersistenceEntities() {
        noClasses()
                .that()
                .resideInAPackage("com.zen.ecs.controller..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.zen.ecs.entity..")
                .because("实体不出入口层，避免持久化结构泄漏成对外接口契约")
                .check(ECS_SERVICE);
    }

    /** 事务边界只放在 service 层：controller 开事务会拉长连接占用，repository 开事务会撕裂业务原子性。 */
    @Test
    void transactionalOnlyAppearsInServiceLayer() {
        noClasses()
                .that()
                .resideOutsideOfPackage("com.zen.ecs.service..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.transaction.annotation.Transactional")
                .because("@Transactional 只允许出现在 com.zen.ecs.service 层，listener/scheduler 只调用不接管")
                .check(ECS_SERVICE);
    }

    /** 分层归属由类名即可判定：名字带角色后缀却落在别的包，上面那几条分层规则会直接漏判。 */
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
                .resideInAPackage("com.zen.ecs." + layerPackage + "..")
                .check(ECS_SERVICE);
    }

    /**
     * 时间源统一：读系统时钟的代码无法把「超时自动离线」拨到阈值之后测，只能真等。
     *
     * <p>{@code new Date()} 由 {@link #neverConstructsJavaUtilDate()} 拦，本条拦另一半——
     * {@code LocalDateTime.now()}/{@code Instant.now()} 看起来是「正确的 java.time」，同样隐式取了系统时钟。
     */
    @Test
    void neverReadsTheSystemClockDirectly() {
        noClasses()
                .should()
                .callMethod(LocalDateTime.class, "now")
                .orShould()
                .callMethod(Instant.class, "now")
                .orShould()
                .callMethod(System.class, "currentTimeMillis")
                .because("时间一律来自注入的 java.time.Clock，否则心跳超时判定测不动")
                .check(ECS_SERVICE);
    }

    /**
     * {@code new Date()} 构造调用被禁；{@code java.util.Date} 类型本身仍然合法——jjwt 的签名只收 Date。
     */
    @Test
    void neverConstructsJavaUtilDate() {
        noClasses()
                .should()
                .callConstructor("java.util.Date")
                .because("用 java.time；java.util.Date 只允许作为 jjwt 的 API 边界短暂出现")
                .check(ECS_SERVICE);
    }

    /** 当前全是构造器注入，固化现状：字段注入让依赖隐形、无法声明 final、脱离 Spring 容器就构造不出可用对象。 */
    @Test
    void neverInjectsByField() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(ECS_SERVICE);
    }

    /**
     * 输出统一走日志框架：P3-1 已把 traceId 打进日志，而标准流输出既拿不到 traceId、也不受日志级别控制。
     * ArchUnit 的这条内置规则同时覆盖 {@code System.out}/{@code System.err} 与 {@code Throwable#printStackTrace}。
     */
    @Test
    void neverWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(ECS_SERVICE);
    }

    /** 服务之间的调用只走 MQ 或 HTTP：编译期依赖别的业务服务会让部署单元互相绑死，也毁掉按模块独立发布。 */
    @Test
    void neverDependsOnOtherServicesOrGateway() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.zen.admin..", "com.zen.gateway..", "com.zen.rcs..", "com.zen.wcs..")
                .because("ecs-service 只依赖 common-core/common-security 这两个公共模块")
                .check(ECS_SERVICE);
    }

    /** 包切片之间无循环依赖：成环会让任一侧改动牵连另一侧，也让后续按包拆分服务变成不可能。 */
    @Test
    void packageSlicesAreFreeOfCycles() {
        slices().matching("com.zen.ecs.(*)..").should().beFreeOfCycles().check(ECS_SERVICE);
    }
}
