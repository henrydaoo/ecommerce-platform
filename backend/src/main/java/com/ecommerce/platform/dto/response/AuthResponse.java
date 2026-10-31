package com.ecommerce.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private UserResponse user;
}
