package com.ecommerce.platform.security;

import com.ecommerce.platform.entity.RefreshToken;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.exception.TokenExpiredOrRevokedException;
import com.ecommerce.platform.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

   
                                                                          
                                                                           
                             
   
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

                                                                                                      
    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        return rawToken;
    }

       
                                                                              
                                                                             
                                                            
       
    @Transactional
    public RotationResult rotate(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new TokenExpiredOrRevokedException();
        }

        RefreshToken existing = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(TokenExpiredOrRevokedException::new);

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredOrRevokedException();
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUser());
        return new RotationResult(existing.getUser(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
    @Transactional public void revokeAllForUser(Long userId) { refreshTokenRepository.revokeAllByUserId(userId); }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(User user, String rawRefreshToken) {
    }
}
