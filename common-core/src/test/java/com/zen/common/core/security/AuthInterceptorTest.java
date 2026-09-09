package com.zen.common.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zen.common.core.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorTest {

  private static final UserPrincipal PRINCIPAL =
      new UserPrincipal(1L, "admin", List.of("admin"), List.of("ADMIN"), "jti-1", null);

  private final AuthInterceptor interceptor = new AuthInterceptor(request -> PRINCIPAL);

  @Test
  void storesPrincipalForTheRequestAndClearsItAfterwards() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isTrue();
    assertThat(UserContext.get()).isEqualTo(PRINCIPAL);

    interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
    assertThat(UserContext.get()).isNull();
  }

  @Test
  void unauthenticatedRequestIsRejectedBeforeReachingTheController() {
    AuthInterceptor anonymous = new AuthInterceptor(request -> null);

    assertThatThrownBy(
            () ->
                anonymous.preHandle(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
        .isInstanceOf(BusinessException.class);
    assertThat(UserContext.get()).isNull();
  }
}
