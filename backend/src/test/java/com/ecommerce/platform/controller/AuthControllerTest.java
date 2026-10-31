package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.request.LoginRequest;
import com.ecommerce.platform.dto.response.AuthResponse;
import com.ecommerce.platform.dto.response.UserResponse;
import com.ecommerce.platform.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void loginSetsSecurelyScopedHttpOnlyRefreshCookie() {
        AuthService authService = mock(AuthService.class);
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");
        when(authService.login(request)).thenReturn(new AuthResponse(
                "access-token",
                "refresh-token",
                new UserResponse(1L, "user@example.com", "Example User", "CUSTOMER")));
        AuthController controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "refreshExpirationMs", 1_209_600_000L);
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", true);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");

        var response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/api/v1/auth");
    }

    @Test
    void refreshWithoutCookieReturnsNoSessionAndExpiresCookie() {
        AuthController controller = new AuthController(mock(AuthService.class));
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", false);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");

        var response = controller.refresh(null, "1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("Max-Age=0", "Path=/api/v1/auth");
    }

    @Test
    void refreshRotatesHttpOnlyCookieAndReturnsNewAccessSession() {
        AuthService authService = mock(AuthService.class);
        when(authService.refresh("old-refresh-token")).thenReturn(new AuthResponse(
                "new-access-token",
                "new-refresh-token",
                new UserResponse(1L, "user@example.com", "Example User", "CUSTOMER")));
        AuthController controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "refreshExpirationMs", 1_209_600_000L);
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", true);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");

        var response = controller.refresh("old-refresh-token", "1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=new-refresh-token", "HttpOnly", "Secure", "SameSite=Lax");
        verify(authService).refresh("old-refresh-token");
    }
}
