package com.qhomebaseapp.repository.token;

import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user AND rt.deviceId = :deviceId")
    void deleteByUserAndDevice(User user, String deviceId);

    Optional<RefreshToken> findByUserAndDeviceId(User user, String deviceId);

    void deleteByUser(User user);
}
