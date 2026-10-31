package com.ecommerce.platform.service.impl;

import com.ecommerce.platform.common.Role;
import com.ecommerce.platform.dto.request.LoginRequest;
import com.ecommerce.platform.dto.request.RegisterRequest;
import com.ecommerce.platform.dto.request.ForgotPasswordRequest;
import com.ecommerce.platform.dto.request.ResetPasswordRequest;
import com.ecommerce.platform.dto.response.AuthResponse;
import com.ecommerce.platform.dto.response.UserResponse;
import com.ecommerce.platform.entity.Cart;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.entity.PasswordResetToken;
import com.ecommerce.platform.event.PasswordResetRequestedEvent;
import com.ecommerce.platform.exception.DuplicateResourceException;
import com.ecommerce.platform.exception.InvalidCredentialsException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.exception.TokenExpiredOrRevokedException;
import com.ecommerce.platform.repository.CartRepository;
import com.ecommerce.platform.repository.UserRepository;
import com.ecommerce.platform.repository.PasswordResetTokenRepository;
import com.ecommerce.platform.security.JwtUtil;
import com.ecommerce.platform.security.RefreshTokenService;
import com.ecommerce.platform.service.AuthService;
import com.ecommerce.platform.service.OutboxNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxNotificationService outboxNotificationService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

                                                                                     
                  
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        outboxNotificationService.userRegistered(user.getId());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);
        User user = result.user();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(accessToken, result.rawRefreshToken(), toUserResponse(user));
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.invalidateUnusedByUserId(user.getId());
            byte[] bytes = new byte[48];
            secureRandom.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash(raw));
            token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
            passwordResetTokenRepository.save(token);
            eventPublisher.publishEvent(
                    new PasswordResetRequestedEvent(user.getEmail(), frontendUrl + "/reset-password?token=" + raw));
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(request.getToken()))
                .orElseThrow(TokenExpiredOrRevokedException::new);
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now()))
            throw new TokenExpiredOrRevokedException();
        token.setUsedAt(Instant.now());
        token.getUser().setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.revokeAllForUser(token.getUser().getId());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken, toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    private String hash(String raw) {
        try {
            return Base64.getEncoder()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
