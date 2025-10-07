package com.qhomebaseapp.service.token;

import com.qhomebaseapp.exception.TokenRefreshException;
import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.token.RefreshTokenRepository;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Xoá token cũ nếu có
        refreshTokenRepository.findByUser(user).ifPresent(old -> {
            log.info("Deleting old refresh token for user: {}", user.getUsername());
            refreshTokenRepository.delete(old);
        });

        // Tạo token mới
        String tokenValue = jwtUtil.generateRefreshToken(user.getUsername());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("New refresh token created for user: {}", user.getUsername());
        return saved;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired and deleted: {}", token.getToken());
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }
        return token;
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            refreshTokenRepository.deleteByUser(user);
            log.info("Deleted refresh token for userId {}", userId);
        });
    }
}
