package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(10);
    private static final int OTP_MAX_REQUESTS = 3;
    private static final int OTP_LENGTH = 6;
    private static final String OTP_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final Map<String, Integer> otpRequestCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpRequestTimes = new ConcurrentHashMap<>();

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existing email {}", email);
            trackOtpRequest(email);
            return;
        }

        if (!canRequestOtp(email)) {
            throw new IllegalStateException("Too many OTP requests. Try again later.");
        }

        User user = userOpt.get();
        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plus(OTP_EXPIRY));
        userRepository.save(user);

        emailService.sendEmail(
                user.getEmail(),
                "Password Reset OTP",
                "Your OTP is: " + otp + " (valid for " + OTP_EXPIRY.toMinutes() + " minutes)"
        );

        log.info("OTP generated for user {}", user.getEmail());
        trackOtpRequest(email);
    }

    public boolean verifyOtp(String email, String otp) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return isOtpValid(user, otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));

        if (!isOtpValid(user, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Password must contain uppercase, lowercase, number, and special character");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        log.info("Password reset successful for user {}", email);
    }

    private boolean canRequestOtp(String email) {
        otpRequestCount.putIfAbsent(email, 0);
        otpRequestTimes.putIfAbsent(email, LocalDateTime.MIN);

        LocalDateTime lastRequest = otpRequestTimes.get(email);
        boolean withinWindow = lastRequest.plus(OTP_EXPIRY).isAfter(LocalDateTime.now());

        if (!withinWindow) {
            otpRequestCount.put(email, 0);
            return true;
        }

        return otpRequestCount.get(email) < OTP_MAX_REQUESTS;
    }

    private void trackOtpRequest(String email) {
        otpRequestTimes.put(email, LocalDateTime.now());
        otpRequestCount.merge(email, 1, Integer::sum);
    }

    private boolean isOtpValid(User user, String otp) {
        if (user.getResetOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        return user.getResetOtp().equalsIgnoreCase(otp);
    }

    private boolean isStrongPassword(String password) {
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    private String generateOtp() {
        StringBuilder builder = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = RANDOM.nextInt(OTP_CHARS.length());
            builder.append(OTP_CHARS.charAt(index));
        }
        return builder.toString();
    }
}

