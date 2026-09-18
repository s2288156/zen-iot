package com.zen.common.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 把只写在注释与 README 里的架构约定变成可执行门禁。
 *
 * <p>只导入主代码（{@link ImportOption.DoNotIncludeTests}），不启动 Spring 上下文，因此可以在没有任何中间件的环境里跑。
 */
@Tag("architecture")
class ArchitectureTest {

    private static final JavaClasses COMMON_CORE = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.zen.common.core");

    /** 对应 common-core/build.gradle.kts 的注释约束:JWT 要与 Phase 2 的 WebFlux 网关复用,绝不能碰 Servlet API。 */
    @Test
    void jwtPackageStaysServletFree() {
        noClasses()
                .that()
                .resideInAPackage("com.zen.common.core.jwt..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("jakarta.servlet..")
                .because("com.zen.common.core.jwt 要能被非 Servlet 的网关复用")
                .check(COMMON_CORE);
    }

    /**
     * 公共模块不得反向依赖业务服务,否则模块边界就断了。
     *
     * <p>包名列表逐模块硬编码:每建一个新模块都要在这里补一条(Phase 0「新模块接入门禁清单」第 3 条),漏一项就是该模块的反向依赖无人拦。
     */
    @Test
    void commonCoreNeverDependsOnBusinessServices() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.zen.admin..", "com.zen.ecs..", "com.zen.gateway..", "com.zen.rcs..", "com.zen.wcs..")
                .because("common-core 是被依赖方,不得引用任何业务服务或网关的类型")
                .check(COMMON_CORE);
    }

    /**
     * 可空性注解只能用 JSpecify:{@code org.springframework.lang.Nullable} 在 Spring Framework 7 已废弃,
     * 目前唯一可用的替代是 {@code org.jspecify.annotations.Nullable}。编译期 -Werror 已能拦住,这里固化成规范。
     */
    @Test
    void neverUsesDeprecatedSpringNullabilityAnnotations() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.lang.Nullable")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.lang.NonNull")
                .because("Spring 7 已废弃这两个注解,需要可空性标注时用 org.jspecify.annotations.*")
                .check(COMMON_CORE);
    }

    /** 公共模块同样只走构造器注入:自动配置类靠方法参数拿依赖,字段注入会让 bean 的装配顺序变得不可读。 */
    @Test
    void neverInjectsByField() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(COMMON_CORE);
    }

    /**
     * 输出统一走日志框架:P3-1 已把 traceId 打进日志,而标准流输出既拿不到 traceId、也不受日志级别控制。
     * ArchUnit 的这条内置规则同时覆盖 {@code System.out}/{@code System.err} 与 {@code Throwable#printStackTrace}。
     */
    @Test
    void neverWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(COMMON_CORE);
    }

    /**
     * 时间源统一:{@code new Date()} 隐式读系统时钟、无法注入测试时钟。
     * 刻意只禁构造调用而不禁整个 {@code java.util.Date}:本模块的 {@link com.zen.common.core.jwt.JwtTokenIssuer}
     * 必须把 {@code Instant} 转成 Date 递给 jjwt,{@code Date.from(Instant)} 属于那道 API 边界。
     */
    @Test
    void neverConstructsJavaUtilDate() {
        noClasses()
                .should()
                .callConstructor("java.util.Date")
                .because("用 java.time;java.util.Date 只允许作为 jjwt 的 API 边界短暂出现")
                .check(COMMON_CORE);
    }

    /** 包切片之间无循环依赖:common-core 会被网关与所有业务服务依赖,成环的包没法在需要时单独拆出。 */
    @Test
    void packageSlicesAreFreeOfCycles() {
        slices().matching("com.zen.common.core.(*)..").should().beFreeOfCycles().check(COMMON_CORE);
    }
}
