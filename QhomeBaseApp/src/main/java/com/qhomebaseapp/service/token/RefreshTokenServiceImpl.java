package com.qhomebaseapp.service.token;

import com.qhomebaseapp.exception.TokenRefreshException;
import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.token.RefreshTokenRepository;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    private final long refreshTokenDurationMs = 24 * 60 * 60 * 1000;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceId) {
        refreshTokenRepository.findByUserAndDeviceId(user, deviceId).ifPresent(oldToken -> {
            blacklistToken(oldToken.getToken(), oldToken.getExpiryDate());
            refreshTokenRepository.deleteByUserAndDevice(user, deviceId); // Xóa trực tiếp
            log.info("Deleted old refresh token for user {} device {}", user.getEmail(), deviceId);
        });

        String tokenValue = jwtUtil.generateRefreshToken(user.getUsername());
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .deviceId(deviceId)
                .token(tokenValue)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        RefreshToken saved = refreshTokenRepository.save(token);
        log.info("Created refresh token for user {} device {}", user.getEmail(), deviceId);
        return saved;
    }

    private void blacklistToken(String token, Instant expiry) {
        try {
            String jti = jwtUtil.extractJti(token);
            long ttl = expiry.getEpochSecond() - Instant.now().getEpochSecond();
            if (ttl > 0) {
                redisTemplate.opsForValue().set("blacklist:" + jti, "revoked", ttl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", token);
        }
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public Optional<RefreshToken> findByUserAndDevice(User user, String deviceId) {
        return refreshTokenRepository.findByUserAndDeviceId(user, deviceId);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            blacklistToken(token.getToken(), token.getExpiryDate());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token đã hết hạn");
        }
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenRepository.save(token);
        return token;
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            refreshTokenRepository.deleteByUser(user);
            log.info("Deleted all refresh tokens for userId {}", userId);
        });
    }

    @Override
    @Transactional
    public void deleteToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            blacklistToken(token, refreshToken.getExpiryDate());
            refreshTokenRepository.delete(refreshToken);
            log.info("Deleted refresh token {}", token);
        });
    }
}
