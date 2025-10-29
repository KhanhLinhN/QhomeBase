package com.qhomebaseapp.controller;
import com.qhomebaseapp.dto.user.LoginRequestDto;
import com.qhomebaseapp.dto.user.ResetPasswordRequest;
import com.qhomebaseapp.dto.token.JwtResponse;
import com.qhomebaseapp.dto.token.TokenRefreshRequest;
import com.qhomebaseapp.exception.TokenRefreshException;
import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.service.security.LoginAttemptService;
import com.qhomebaseapp.service.token.RefreshTokenService;
import com.qhomebaseapp.service.user.EmailService;
import com.qhomebaseapp.service.user.UserService;
import com.qhomebaseapp.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final Map<String, Integer> otpRequestCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpRequestTime = new ConcurrentHashMap<>();
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MAX_REQUESTS = 3;
    private static final int OTP_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();
    private static final String OTP_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest,
                                   @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required"));
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email format"));
        }

        if (loginAttemptService.isBlocked(email))
            return ResponseEntity.status(429).body(Map.of("message", "Too many failed attempts. Try later."));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            loginAttemptService.loginSucceeded(email);
        } catch (AuthenticationException e) {
            loginAttemptService.loginFailed(email);
            log.warn("Login failed for {}", email);
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        String accessToken = jwtUtil.generateAccessToken(user);
        deviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId : "default";
        final RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceId);

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .username(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(jwtResponse);
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request,
                                          @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {

        String oldToken = request.getRefreshToken();
        deviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId : "default";

        RefreshToken token = refreshTokenService.findByToken(oldToken)
                .orElseThrow(() -> new TokenRefreshException(oldToken, "Refresh token không tồn tại"));

        refreshTokenService.verifyExpiration(token);

        refreshTokenService.deleteToken(oldToken);
        final RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(token.getUser(), deviceId);

        String newAccessToken = jwtUtil.generateAccessToken(token.getUser());

        log.info("Refresh token rotated for user {} device {}", token.getUser().getEmail(), deviceId);

        JwtResponse response = JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .userId(token.getUser().getId())
                .username(token.getUser().getEmail())
                .role(token.getUser().getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        final String finalDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId : "default";

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtUtil.blacklistAccessToken(token);
            log.info("Access token blacklisted for user {}", userDetails.getUsername());
        }

        userService.getUserByEmail(userDetails.getUsername())
                .flatMap(user -> refreshTokenService.findByUserAndDevice(user, finalDeviceId))
                .ifPresent(rt -> refreshTokenService.deleteToken(rt.getToken()));

        SecurityContextHolder.clearContext();
        log.info("User {} logged out device {}", userDetails.getUsername(), finalDeviceId);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Email required"));

        if (!canRequestOtp(email))
            return ResponseEntity.status(429).body(Map.of("message", "Too many OTP requests. Try later."));

        userService.getUserByEmail(email).ifPresent(user -> {
            final String otp = generateOtp();
            user.setResetOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            userService.saveUser(user);
            emailService.sendEmail(user.getEmail(), "Password Reset OTP",
                    "Your OTP is: " + otp + " (valid for 10 minutes)");
        });

        otpRequestCount.merge(email, 1, Integer::sum);
        otpRequestTime.put(email, LocalDateTime.now());
        log.info("OTP requested for {}", email);

        return ResponseEntity.ok(Map.of("message", "If the email exists, OTP has been sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP required"));

        Optional<User> userOpt = userService.getUserByEmail(email);

        if (userOpt.isEmpty() || isOtpInvalid(userOpt.get(), otp))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));

        return ResponseEntity.ok(Map.of("message", "OTP valid"));
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<?> confirmReset(@RequestBody ResetPasswordRequest resetRequest) {
        String email = resetRequest.getEmail();
        String otp = resetRequest.getOtp();
        String newPassword = resetRequest.getNewPassword();

        if (email == null || otp == null || newPassword == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email, OTP and new password required"));

        Optional<User> userOpt = userService.getUserByEmail(email);

        if (userOpt.isEmpty() || isOtpInvalid(userOpt.get(), otp))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));

        User user = userOpt.get();

        if (!isStrongPassword(newPassword))
            return ResponseEntity.badRequest().body(Map.of("message", "Password must contain uppercase, lowercase, number, and special character"));

        if (userService.checkPassword(newPassword, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be different from old password"));

        user.setPassword(userService.encodePassword(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userService.saveUser(user);

        log.info("Password reset successful for {}", email);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    private boolean canRequestOtp(String email) {
        otpRequestCount.putIfAbsent(email, 0);
        otpRequestTime.putIfAbsent(email, LocalDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES));
        LocalDateTime lastRequest = otpRequestTime.get(email);
        return otpRequestCount.get(email) < OTP_MAX_REQUESTS
                || lastRequest.plusMinutes(OTP_EXPIRY_MINUTES).isBefore(LocalDateTime.now());
    }

    private boolean isOtpInvalid(User user, String otp) {
        return user.getResetOtp() == null
                || !user.getResetOtp().equalsIgnoreCase(otp)
                || user.getOtpExpiry() == null
                || user.getOtpExpiry().isBefore(LocalDateTime.now());
    }

    private boolean isStrongPassword(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        return pattern.matcher(password).matches();
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++)
            sb.append(OTP_CHARS.charAt(random.nextInt(OTP_CHARS.length())));
        return sb.toString();
    }
}