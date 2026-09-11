package com.zen.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zen.admin.dto.LoginRequest;
import com.zen.admin.service.AuthService;
import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import com.zen.common.core.jwt.TokenPair;
import com.zen.common.core.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link AuthController} 的 Web 切片测试：standalone MockMvc，只注册被测 controller 与 {@link GlobalExceptionHandler}。
 *
 * <p>不使用 {@code @WebMvcTest}：common-core 的安全自动配置（{@code ZenSecurityAutoConfiguration}）会在切片上下文中装配拦截器，
 * 对未携带 Token 的测试请求直接抛 401。standalone 方式绕过该干扰，只验证 controller 的参数校验与异常透出契约。
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- /auth/login ----------

    @Test
    void loginReturnsTokenPairOnValidRequest() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new TokenPair("access-xyz", "refresh-xyz"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "admin", "password": "pwd123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-xyz"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-xyz"));
    }

    @Test
    void loginWithMissingUsernameReturns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "pwd123" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("username")));
    }

    @Test
    void loginWithBlankPasswordReturns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "admin", "password": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));
    }

    @Test
    void loginWithBadCredentialsReturns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "admin", "password": "wrong" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    // ---------- /auth/refresh ----------

    @Test
    void refreshReturnsNewTokenPair() throws Exception {
        when(authService.refresh("old-refresh")).thenReturn(new TokenPair("access-new", "refresh-new"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "old-refresh" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-new"));
    }

    @Test
    void refreshWithMissingTokenReturns400() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void refreshWithRevokedTokenReturns401() throws Exception {
        when(authService.refresh("revoked")).thenThrow(new BusinessException(GlobalErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "revoked" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ---------- /auth/logout ----------

    @Test
    void logoutReturnsSuccessOnValidRequest() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "some-refresh" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void logoutWithMissingTokenReturns400() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
