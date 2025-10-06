package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.user.LoginRequestDto;
import com.qhomebaseapp.dto.user.ResetPasswordRequest;
import com.qhomebaseapp.dto.user.UserResponse;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.service.user.EmailService;
import com.qhomebaseapp.service.user.UserService;
import com.qhomebaseapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

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


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password required"));
        }

        if (loginLockTime.containsKey(email) &&
                loginLockTime.get(email).isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many failed login attempts. Try later."));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (AuthenticationException e) {
            loginFailCount.put(email, loginFailCount.getOrDefault(email, 0) + 1);
            if (loginFailCount.get(email) >= LOGIN_MAX_FAILS) {
                loginLockTime.put(email, LocalDateTime.now().plusMinutes(LOGIN_LOCK_MINUTES));
                loginFailCount.put(email, 0);
            }
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }

        loginFailCount.put(email, 0);

        User user = userService.getUserByEmail(email).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail());

        UserResponse response = new UserResponse(user.getId(), user.getEmail(), user.getUsername(), token);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }


    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email required"));
        }

        otpRequestCount.putIfAbsent(email, 0);
        otpRequestTime.putIfAbsent(email, LocalDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES));

        LocalDateTime lastRequest = otpRequestTime.get(email);
        if (otpRequestCount.get(email) >= OTP_MAX_REQUESTS &&
                lastRequest.plusMinutes(OTP_EXPIRY_MINUTES).isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many OTP requests. Try later."));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String otp = generateOtp(8);
            user.setResetOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            userService.saveUser(user);

            emailService.sendEmail(user.getEmail(), "Password Reset OTP",
                    "Your OTP is: " + otp + " (valid for 10 minutes)");
            otpRequestCount.put(email, otpRequestCount.get(email) + 1);
            otpRequestTime.put(email, LocalDateTime.now());
        }

        return ResponseEntity.ok(Map.of("message", "If the email exists, OTP has been sent"));
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP required"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP"));
        }

        User user = userOpt.get();
        if (user.getResetOtp() == null || !user.getResetOtp().equalsIgnoreCase(otp) ||
                user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));
        }

        return ResponseEntity.ok(Map.of("message", "OTP valid"));
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<?> confirmReset(@RequestBody ResetPasswordRequest resetRequest) {
        String email = resetRequest.getEmail();
        String otp = resetRequest.getOtp();
        String newPassword = resetRequest.getNewPassword();

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email, OTP and new password required"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP or email"));
        }

        User user = userOpt.get();
        if (user.getResetOtp() == null || !user.getResetOtp().equalsIgnoreCase(otp) ||
                user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));
        }

        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        if (!pattern.matcher(newPassword).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Password must be at least 8 characters, include uppercase, lowercase, digit and special character"));
        }

        if (userService.checkPassword(newPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be different from old password"));
        }

        user.setPassword(userService.encodePassword(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userService.saveUser(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }


    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(OTP_CHARS.charAt(random.nextInt(OTP_CHARS.length())));
        }
        return sb.toString();
    }
}
