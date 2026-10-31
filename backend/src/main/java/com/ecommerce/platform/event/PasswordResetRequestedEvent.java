package com.ecommerce.platform.event;

public record PasswordResetRequestedEvent(String email, String resetUrl) {
}
