package com.ecommerce.platform.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank
    private String token;
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
