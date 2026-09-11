package com.zen.common.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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

    /** 公共模块不得反向依赖业务服务,否则模块边界就断了。 */
    @Test
    void commonCoreNeverDependsOnBusinessServices() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.zen.admin..", "com.zen.ecs..", "com.zen.rcs..", "com.zen.wcs..")
                .because("common-core 是被依赖方,不得引用任何业务服务的类型")
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
}
