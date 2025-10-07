package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.user.LoginRequestDto;
import com.qhomebaseapp.dto.user.ResetPasswordRequest;
import com.qhomebaseapp.dto.token.JwtResponse;
import com.qhomebaseapp.dto.token.TokenRefreshRequest;
import com.qhomebaseapp.exception.TokenRefreshException;
import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;
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

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private final Map<String, Integer> otpRequestCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpRequestTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> loginFailCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> loginLockTime = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MAX_REQUESTS = 3;
    private static final int LOGIN_MAX_FAILS = 5;
    private static final int LOGIN_LOCK_MINUTES = 15;
    private static final SecureRandom random = new SecureRandom();
    private static final String OTP_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // ======================== LOGIN ======================== //
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        if (email == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password required"));

        if (isAccountLocked(email))
            return ResponseEntity.status(429).body(Map.of("message", "Too many failed attempts. Try later."));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (AuthenticationException e) {
            registerLoginFailure(email);
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }

        resetLoginFail(email);

        // 🔹 Đổi sang getUserByEmail()
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .username(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(jwtResponse);
    }

    // ======================== REFRESH TOKEN ======================== //
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(verifiedToken -> {
                    String newAccessToken = jwtUtil.generateAccessToken(verifiedToken.getUser());
                    return ResponseEntity.ok(JwtResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(refreshToken)
                            .userId(verifiedToken.getUser().getId())
                            .username(verifiedToken.getUser().getEmail())
                            .role(verifiedToken.getUser().getRole())
                            .build());
                })
                .orElseThrow(() -> new TokenRefreshException(refreshToken, "Refresh token không tồn tại hoặc đã hết hạn"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails))
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));

        userService.getUserByEmail(userDetails.getUsername())
                .ifPresent(user -> refreshTokenService.deleteByUserId(user.getId()));

        SecurityContextHolder.clearContext();
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
            String otp = generateOtp(8);
            user.setResetOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            userService.saveUser(user);
            emailService.sendEmail(user.getEmail(), "Password Reset OTP",
                    "Your OTP is: " + otp + " (valid for 10 minutes)");
        });

        otpRequestCount.merge(email, 1, Integer::sum);
        otpRequestTime.put(email, LocalDateTime.now());
        return ResponseEntity.ok(Map.of("message", "If the email exists, OTP has been sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP required"));

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty() || !isValidOtp(userOpt.get(), otp))
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
        if (userOpt.isEmpty() || !isValidOtp(userOpt.get(), otp))
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

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    private boolean isAccountLocked(String email) {
        return loginLockTime.containsKey(email) && loginLockTime.get(email).isAfter(LocalDateTime.now());
    }

    private void registerLoginFailure(String email) {
        loginFailCount.merge(email, 1, Integer::sum);
        if (loginFailCount.get(email) >= LOGIN_MAX_FAILS) {
            loginLockTime.put(email, LocalDateTime.now().plusMinutes(LOGIN_LOCK_MINUTES));
            loginFailCount.put(email, 0);
        }
    }

    private void resetLoginFail(String email) {
        loginFailCount.put(email, 0);
    }

    private boolean canRequestOtp(String email) {
        otpRequestCount.putIfAbsent(email, 0);
        otpRequestTime.putIfAbsent(email, LocalDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES));
        LocalDateTime lastRequest = otpRequestTime.get(email);
        return otpRequestCount.get(email) < OTP_MAX_REQUESTS
                || lastRequest.plusMinutes(OTP_EXPIRY_MINUTES).isBefore(LocalDateTime.now());
    }

    private boolean isValidOtp(User user, String otp) {
        return user.getResetOtp() != null
                && user.getResetOtp().equalsIgnoreCase(otp)
                && user.getOtpExpiry() != null
                && user.getOtpExpiry().isAfter(LocalDateTime.now());
    }

    private boolean isStrongPassword(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        return pattern.matcher(password).matches();
    }

    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(OTP_CHARS.charAt(random.nextInt(OTP_CHARS.length())));
        return sb.toString();
    }
}
