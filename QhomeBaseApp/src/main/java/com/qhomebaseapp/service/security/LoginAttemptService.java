package com.qhomebaseapp.service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Integer> redisTemplateInt;

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(150);

    public void loginFailed(String email) {
        Integer attempts = redisTemplateInt.opsForValue().get(email);
        attempts = (attempts == null) ? 1 : attempts + 1;
        redisTemplateInt.opsForValue().set(email, attempts, LOCK_TIME);
    }

    public void loginSucceeded(String email) {
        redisTemplateInt.delete(email);
    }

    public boolean isBlocked(String email) {
        Integer attempts = redisTemplateInt.opsForValue().get(email);
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }
}
