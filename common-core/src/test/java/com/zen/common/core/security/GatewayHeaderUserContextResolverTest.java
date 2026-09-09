package com.zen.common.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GatewayHeaderUserContextResolverTest {

    private final GatewayHeaderUserContextResolver resolver = new GatewayHeaderUserContextResolver();

    @Test
    void readsIdentityFromGatewayHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayHeaderUserContextResolver.USER_ID_HEADER, "7");
        request.addHeader(GatewayHeaderUserContextResolver.USERNAME_HEADER, "demo");
        request.addHeader(GatewayHeaderUserContextResolver.ROLES_HEADER, "user");
        request.addHeader(GatewayHeaderUserContextResolver.MODULES_HEADER, "ECS, RCS,wcs");

        assertThat(resolver.resolve(request))
                .extracting(
                        UserPrincipal::userId, UserPrincipal::username, UserPrincipal::roles, UserPrincipal::modules)
                .containsExactly(7L, "demo", List.of("user"), List.of("ECS", "RCS", "wcs"));
    }

    @Test
    void missingOrMalformedUserIdMeansUnauthenticated() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();

        MockHttpServletRequest blank = new MockHttpServletRequest();
        blank.addHeader(GatewayHeaderUserContextResolver.USER_ID_HEADER, "  ");
        blank.addHeader(GatewayHeaderUserContextResolver.USERNAME_HEADER, "attacker");
        assertThat(resolver.resolve(blank)).isNull();

        MockHttpServletRequest notANumber = new MockHttpServletRequest();
        notANumber.addHeader(GatewayHeaderUserContextResolver.USER_ID_HEADER, "1 or 2=2");
        assertThat(resolver.resolve(notANumber)).isNull();
    }

    @Test
    void absentRoleAndModuleHeadersBindToEmptyCollections() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayHeaderUserContextResolver.USER_ID_HEADER, "7");

        UserPrincipal principal = resolver.resolve(request);

        assertThat(principal.roles()).isEmpty();
        assertThat(principal.modules()).isEmpty();
        assertThat(principal.jti()).isNull();
        assertThat(principal.remainingTtl()).isNull();
    }
}
