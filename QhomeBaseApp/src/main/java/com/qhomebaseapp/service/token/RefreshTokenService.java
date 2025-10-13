package com.qhomebaseapp.service.token;

import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user, String deviceId);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyExpiration(RefreshToken token);

    void deleteByUserId(Long userId);

    void deleteToken(String token);

    Optional<RefreshToken> findByUserAndDevice(User user, String deviceId);
}
