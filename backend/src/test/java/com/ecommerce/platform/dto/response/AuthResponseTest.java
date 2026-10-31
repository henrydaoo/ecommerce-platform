package com.ecommerce.platform.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void refreshTokenIsNotSerializedToTheBrowser() throws Exception {
        AuthResponse response = new AuthResponse(
                "access-value",
                "refresh-secret-value",
                new UserResponse(1L, "user@example.com", "Example User", "CUSTOMER"));

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("accessToken", "user@example.com");
        assertThat(json).doesNotContain("refreshToken", "refresh-secret-value");
    }
}
