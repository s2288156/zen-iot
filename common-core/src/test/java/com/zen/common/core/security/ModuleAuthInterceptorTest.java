package com.zen.common.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zen.common.core.exception.BusinessException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.method.HandlerMethod;

class ModuleAuthInterceptorTest {

  private final ModuleAuthInterceptor interceptor = new ModuleAuthInterceptor();

  @Test
  void nonHandlerEndpointsArePassedThrough() {
    assertThat(interceptor.preHandle(null, null, new Object())).isTrue();
  }

  @Test
  void unannotatedMethodsOnlyRequireLogin() {
    UserContext.set(principal("admin"));

    assertThat(interceptor.preHandle(null, null, handler("open"))).isTrue();

    UserContext.clear();
  }

  @Test
  void annotatedMethodWithoutAnyContextIsUnauthorized() {
    assertThatThrownBy(() -> interceptor.preHandle(null, null, handler("admin")))
        .isInstanceOf(BusinessException.class);
  }

  @ParameterizedTest
  @MethodSource("cases")
  void annotatedMethodsAreCheckedAgainstTheTokenModuleSnapshot(
      List<String> modules, Class<? extends Exception> expected) {
    UserContext.set(principal(modules.toArray(String[]::new)));

    try {
      if (expected == null) {
        assertThat(interceptor.preHandle(null, null, handler("admin"))).isTrue();
      } else {
        assertThatThrownBy(() -> interceptor.preHandle(null, null, handler("admin")))
            .isInstanceOf(expected);
      }
    } finally {
      UserContext.clear();
    }
  }

  static Stream<Arguments> cases() {
    return Stream.of(
        // 码值是小写：种子数据与 modules claim 都用 admin/ecs/rcs/wcs
        Arguments.of(List.of("admin"), null),
        Arguments.of(List.of("ecs", "wcs"), BusinessException.class),
        Arguments.of(List.of("ADMIN"), BusinessException.class),
        Arguments.of(List.of(), BusinessException.class));
  }

  private HandlerMethod handler(String method) {
    try {
      return new HandlerMethod(new Endpoint(), Endpoint.class.getMethod(method));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  private UserPrincipal principal(String... modules) {
    return new UserPrincipal(1L, "admin", List.of("admin"), List.of(modules), "jti", null);
  }

  static class Endpoint {

    @RequireModule(ModuleCode.ADMIN)
    public void admin() {}

    public void open() {}
  }
}
