package com.zen.gateway;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 网关模块的架构门禁。
 *
 * <p>WebFlux 网关没有 JPA 层，不套用 {@code controller → service → repository → entity} 四层（Phase 0「新模块接入门禁清单」
 * 第 2 条），但通用四条（禁字段注入、禁标准流、禁 {@code new Date()}、包切片无环）原样保留，另加三条只属于反应式网关的约束——它们正是
 * Phase 2「关键决策」里「依赖隔离」与「Redis 必须走反应式」两条的自动化版本。摘掉任一条都会退化成一句口头约定。
 */
@Tag("architecture")
class ArchitectureTest {

    private static final JavaClasses GATEWAY_SERVICE = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.zen.gateway");

    /** 网关跑在 Netty 上：Servlet/MVC 栈一旦回到编译类路径，Boot 就把应用判成 Servlet 类型，启动直接失败。 */
    @Test
    void neverUsesServletOrWebMvcStack() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("jakarta.servlet..", "jakarta.validation..", "org.springframework.web.servlet..")
                .because("网关是 WebFlux 应用，Servlet 栈上类路径即启动失败")
                .check(GATEWAY_SERVICE);
    }

    /** 网关没有数据库，也不该有：data-jpa 上类路径会让 DataSourceAutoConfiguration 在无数据源时炸。 */
    @Test
    void neverUsesPersistenceStack() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..", "org.springframework.data.jpa..", "org.hibernate..")
                .because("网关不落库，持久化栈由 exclude 排掉，不许再引回来")
                .check(GATEWAY_SERVICE);
    }

    /**
     * 网关不得依赖 {@code common-core}：那里剩下的每一类都绑定 Servlet（拦截器、{@code WebMvcConfigurer} 自动配置、
     * JPA 实体基类、{@code GlobalExceptionHandler}）。拆出 {@code common-security} 之后这条从「建议」变成了断言——
     * 网关的构建脚本里已没有任何 {@code exclude}，一旦有人重新引回 {@code common-core}，本条立刻变红，
     * 而失效方式原本要等真启动才看得见。
     *
     * <p>{@code UserContext} 与 Servlet 无关（它是 ThreadLocal），所以单独点名：反应式链路里一个请求会跨多个线程，
     * ThreadLocal 身份上下文要么读到上一个请求的用户、要么读到 null。
     */
    @Test
    void neverDependsOnCommonCore() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.zen.common.core..", "com.zen.common.core.web..", "com.zen.common.core.entity..")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Interceptor")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.zen.common.security.auth.UserContext")
                .because("网关是 WebFlux 应用，Servlet 拦截器与 ThreadLocal 身份上下文都不能复用")
                .check(GATEWAY_SERVICE);
    }

    /**
     * 黑名单查询必须走反应式：命令式 {@code StringRedisTemplate#hasKey} 在 EventLoop 上「能跑」，高并发下把事件循环堵死且没有任何报错。
     * 这里按 FQN 精确拦住两个命令式模板（{@code ReactiveStringRedisTemplate} 同包，故不能整包禁）。
     */
    @Test
    void neverUsesImperativeRedisTemplate() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.data.redis.core.StringRedisTemplate")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.data.redis.core.RedisTemplate")
                .because("Netty EventLoop 上只能用 ReactiveStringRedisTemplate")
                .check(GATEWAY_SERVICE);
    }

    /** 反应式链路里任何 {@code block()}/{@code blockFirst()}/{@code blockLast()} 都会占用事件循环，与上面那条同源的失效模式。 */
    @Test
    void neverBlocksReactivePipeline() {
        noClasses()
                .should()
                .callMethodWhere(BLOCKING_REACTOR_OPERATOR)
                .because("网关全程反应式，阻塞式取值的算子一律禁止（P3-2 BlockHound 要拦的第一类问题）")
                .check(GATEWAY_SERVICE);
    }

    /** 按「被调用方是 Mono/Flux 且方法名以 block 开头」匹配，比逐个列重载签名可靠。 */
    private static final DescribedPredicate<JavaMethodCall> BLOCKING_REACTOR_OPERATOR =
            new DescribedPredicate<JavaMethodCall>("被调用方是 reactor 的 Mono/Flux 且方法名以 block 开头") {
                @Override
                public boolean test(JavaMethodCall call) {
                    String owner = call.getTarget().getOwner().getFullName();
                    return (owner.equals("reactor.core.publisher.Mono") || owner.equals("reactor.core.publisher.Flux"))
                            && call.getName().startsWith("block");
                }
            };

    @Test
    void neverInjectsByField() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(GATEWAY_SERVICE);
    }

    /** 输出统一走日志框架：P3-1 已把 traceId 打进日志，标准流输出既拿不到 traceId 也不受级别控制。 */
    @Test
    void neverWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(GATEWAY_SERVICE);
    }

    /** 时间源统一 {@code java.time}：{@code new Date()} 隐式读系统时钟、无法注入测试时钟。 */
    @Test
    void neverConstructsJavaUtilDate() {
        noClasses()
                .should()
                .callConstructor("java.util.Date")
                .because("用 java.time；java.util.Date 只允许作为 jjwt 的 API 边界短暂出现")
                .check(GATEWAY_SERVICE);
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
                .check(GATEWAY_SERVICE);
    }

    /** 包切片之间无循环依赖：成环会让任一侧改动牵连另一侧，也让后续按包拆分服务变成不可能。 */
    @Test
    void packageSlicesAreFreeOfCycles() {
        slices().matching("com.zen.gateway.(*)..").should().beFreeOfCycles().check(GATEWAY_SERVICE);
    }
}
