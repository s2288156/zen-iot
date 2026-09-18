package com.zen.common.security;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Web 无关安全内核的边界门禁。
 *
 * <p>本模块唯一的职责是让 JWT 与统一响应/错误码契约**不绑定传输层**：反应式网关直接依赖它即可复用 Phase 1 的验签代码，
 * 不必像拆模块前那样在消费方逐条 {@code exclude} {@code common-core} 的 {@code api} 传递依赖。因此这里的第一条规则
 * 就是这条存在理由本身——它一旦破功，网关就会在启动期被判成 Servlet 应用（或因无数据源而炸 {@code DataSourceAutoConfiguration}），
 * 而破坏方式只是「有人往这个模块加了个用 {@code HttpServletRequest} 的类」，编译与 IDE 都不会提醒。
 */
@Tag("architecture")
class ArchitectureTest {

    private static final JavaClasses COMMON_SECURITY = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.zen.common.security");

    /**
     * 反「静默失效」哨兵：ArchUnit 的 {@code noClasses()} 在**一个类都没导入**时也判定通过，所以包根改名、误删目录、
     * 或 {@code importPackages} 写错，都会让上面/下面所有规则一起变成零断言绿（Phase 0「新模块接入门禁清单」第 2 条点名的
     * 正是这种失效）。本模块类数随内容增长只会更多，20 是拆模块时实测值 19 的下一档。
     */
    @Test
    void importedClassesAreNeverEmpty() {
        assertThat(COMMON_SECURITY.size()).isGreaterThan(20);
    }

    /**
     * 整个模块不得依赖 Servlet、Spring Web（两种传输栈）、持久化或 Spring Data。
     *
     * <p>取代拆模块前 {@code common-core} 里那条只盯 {@code jakarta.servlet} 的 {@code jwtPackageStaysServletFree()}：
     * 包已经独立成模块，规则也按模块收紧——只禁 Servlet 挡不住 {@code spring-webmvc}、JPA 与 validation 混进来。
     */
    @Test
    void moduleStaysFreeOfWebAndPersistence() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.servlet..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "org.springframework.web..",
                        "org.springframework.data..",
                        "org.springframework.transaction..",
                        "org.hibernate..")
                .because("本模块要能被 WebFlux 网关复用，绑定任一传输栈或持久化层都会把 exclude 清单还给消费方")
                .check(COMMON_SECURITY);
    }

    /** 依赖方向单向：common-core → common-security。反向引用会让「内核」失去独立可复用性。 */
    @Test
    void neverDependsOnCommonCore() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.zen.common.core..")
                .because("common-security 是被依赖方，反向引用会把模块边界重新焊死")
                .check(COMMON_SECURITY);
    }

    /** 不依赖任何业务模块或网关：身份头/错误码契约是所有服务的公共输入，反过来依赖就是循环。 */
    @Test
    void neverDependsOnServicesOrGateway() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.zen.admin..", "com.zen.ecs..", "com.zen.gateway..", "com.zen.rcs..", "com.zen.wcs..")
                .because("公共内核不得引用任何业务服务或网关的类型")
                .check(COMMON_SECURITY);
    }

    /**
     * {@code new Date()} 构造调用被禁；{@code java.util.Date} 类型本身仍然合法——jjwt 的
     * {@code issuedAt}/{@code expiration} 签名只收 Date，{@link com.zen.common.security.jwt.JwtTokenIssuer}
     * 用 {@code Date.from(Instant)} 跨那道 API 边界。
     */
    @Test
    void neverConstructsJavaUtilDate() {
        noClasses()
                .should()
                .callConstructor("java.util.Date")
                .because("时间源统一 java.time；java.util.Date 只允许作为 jjwt 的 API 边界短暂出现")
                .check(COMMON_SECURITY);
    }

    /** 可空性注解只能用 JSpecify：Spring 7 的两个注解被 -Werror 与本条双重拦。 */
    @Test
    void neverUsesDeprecatedSpringNullabilityAnnotations() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.lang.Nullable")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.lang.NonNull")
                .because("Spring 7 已废弃这两个注解，需要可空性标注时用 org.jspecify.annotations.*")
                .check(COMMON_SECURITY);
    }

    @Test
    void neverInjectsByField() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(COMMON_SECURITY);
    }

    /** 输出统一走日志框架：P3-1 已把 traceId 打进日志，标准流输出拿不到 traceId 也不受级别控制。 */
    @Test
    void neverWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(COMMON_SECURITY);
    }

    /** 包切片之间无循环依赖：内核模块一旦成环就再也拆不开，也无法被非 Web 组件单独复用。 */
    @Test
    void packageSlicesAreFreeOfCycles() {
        slices().matching("com.zen.common.security.(*)..")
                .should()
                .beFreeOfCycles()
                .check(COMMON_SECURITY);
    }
}
