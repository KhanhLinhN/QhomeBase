//package com.qhomebaseapp.loginservice.controller;
//
//import com.qhomebaseapp.loginservice.dto.ResetPasswordRequest;
//import com.qhomebaseapp.loginservice.entity.User;
//import com.qhomebaseapp.loginservice.service.EmailService;
//import com.qhomebaseapp.loginservice.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Random;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//    private final EmailService emailService;
//
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody User loginRequest) {
//        Optional<User> user = userService.getUserByEmail(loginRequest.getEmail());
//
//        if (user.isPresent()) {
//            User existing = user.get();
//
//            if (userService.checkPassword(loginRequest.getPassword(), existing.getPassword())) {
//                return ResponseEntity.ok(existing);
//            }
//        }
//
//        return ResponseEntity.status(401).body("Invalid email or password");
//    }
//
//    @PostMapping("/logout")
//    public ResponseEntity<?> logout() {
//        // logout đơn giản (sau này có JWT thì revoke token)
//        return ResponseEntity.ok("Logged out successfully");
//    }
//
//    @PostMapping("/reset-password")
//    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
//        Optional<User> userOpt = userService.getUserByEmail(resetRequest.getEmail());
//
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body("User not found");
//        }
//
//        User user = userOpt.get();
//
//        // Check OTP trước khi reset
//        if (user.getResetOtp() == null || !user.getResetOtp().equals(resetRequest.getOtp())) {
//            return ResponseEntity.status(401).body("Invalid OTP");
//        }
//        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
//            return ResponseEntity.status(401).body("OTP expired");
//        }
//
//        // Update password
//        String hashedPassword = userService.encodePassword(resetRequest.getNewPassword());
//        user.setPassword(hashedPassword);
//        user.setResetOtp(null);   // clear OTP sau khi dùng
//        user.setOtpExpiry(null);  // clear expiry
//        userService.saveUser(user);
//
//        return ResponseEntity.ok("Password updated successfully");
//    }
//
//    @PostMapping("/request-reset")
//    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
//        String email = body.get("email");
//        Optional<User> userOpt = userService.getUserByEmail(email);
//
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body("Email not found");
//        }
//
//        User user = userOpt.get();
//        String otp = String.format("%06d", new Random().nextInt(999999));
//        user.setResetOtp(otp);
//        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
//        userService.saveUser(user);
//
//        emailService.sendEmail(user.getEmail(), "Password Reset OTP",
//                "Your OTP is: " + otp + " (valid for 3 minutes)");
//
//        return ResponseEntity.ok("OTP sent to email");
//    }
//
//
//    @PostMapping("/confirm-reset")
//    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> request) {
//        String email = request.get("email");
//        String otp = request.get("otp");
//        String newPassword = request.get("newPassword");
//
//        Optional<User> user = userService.getUserByEmail(email);
//
//        if (user.isPresent()) {
//            User existing = user.get();
//            if (otp.equals(existing.getResetOtp()) &&
//                    existing.getOtpExpiry().isAfter(LocalDateTime.now())) {
//
//                existing.setPassword(userService.encodePassword(newPassword));
//                existing.setResetOtp(null);
//                existing.setOtpExpiry(null);
//                userService.saveUser(existing);
//
//                return ResponseEntity.ok("Password updated successfully");
//            } else {
//                return ResponseEntity.badRequest().body("Invalid or expired OTP");
//            }
//        }
//
//        return ResponseEntity.badRequest().body("Email not found");
//    }
//
//    @PostMapping("/verify-otp")
//    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
//        String email = body.get("email");
//        String otp = body.get("otp");
//
//        Optional<User> userOpt = userService.getUserByEmail(email);
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body("Email not found");
//        }
//
//        User user = userOpt.get();
//        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
//            return ResponseEntity.status(401).body("Invalid OTP");
//        }
//
//        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
//            return ResponseEntity.status(401).body("OTP expired");
//        }
//
//        return ResponseEntity.ok("OTP valid, you can reset password");
//    }
//
//}

package com.qhomebaseapp.loginservice.controller;

import com.qhomebaseapp.loginservice.dto.ResetPasswordRequest;
import com.qhomebaseapp.loginservice.dto.UserResponse;
import com.qhomebaseapp.loginservice.entity.User;
import com.qhomebaseapp.loginservice.service.EmailService;
import com.qhomebaseapp.loginservice.service.UserService;
import com.qhomebaseapp.loginservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // Rate limiting maps
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

    // ---------------- LOGIN ----------------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password required"));
        }

        // Check login lock
        if (loginLockTime.containsKey(email) &&
                loginLockTime.get(email).isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many failed login attempts. Try later."));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty() || !userService.checkPassword(password, userOpt.get().getPassword())) {
            loginFailCount.put(email, loginFailCount.getOrDefault(email, 0) + 1);
            if (loginFailCount.get(email) >= LOGIN_MAX_FAILS) {
                loginLockTime.put(email, LocalDateTime.now().plusMinutes(LOGIN_LOCK_MINUTES));
                loginFailCount.put(email, 0);
            }
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }

        // Reset fail count on successful login
        loginFailCount.put(email, 0);

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getEmail());

        // Return DTO, không lộ password
        UserResponse response = new UserResponse(user.getId(), user.getEmail(), user.getUsername(), token);
        return ResponseEntity.ok(response);
    }

    // ---------------- LOGOUT ----------------

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // TODO: nếu dùng JWT, client xóa token là đủ
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ---------------- REQUEST OTP ----------------

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email required"));
        }

        // Rate limit OTP request
        otpRequestCount.putIfAbsent(email, 0);
        otpRequestTime.putIfAbsent(email, LocalDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES));

        LocalDateTime lastRequest = otpRequestTime.get(email);
        if (otpRequestCount.get(email) >= OTP_MAX_REQUESTS &&
                lastRequest.plusMinutes(OTP_EXPIRY_MINUTES).isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many OTP requests. Try later."));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);

        // Không tiết lộ email tồn tại
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String otp = generateOtp(8);
            user.setResetOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            userService.saveUser(user);

            emailService.sendEmail(user.getEmail(), "Password Reset OTP",
                    "Your OTP is: " + otp + " (valid for 10 minutes)");

            // Update rate limiting
            otpRequestCount.put(email, otpRequestCount.get(email) + 1);
            otpRequestTime.put(email, LocalDateTime.now());
        }

        // Luôn trả message chung
        return ResponseEntity.ok(Map.of("message", "If the email exists, OTP has been sent"));
    }

    // ---------------- VERIFY OTP ----------------

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP required"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            // Không tiết lộ email
            return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP"));
        }

        User user = userOpt.get();
        if (user.getResetOtp() == null || !user.getResetOtp().equalsIgnoreCase(otp) ||
                user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));
        }

        return ResponseEntity.ok(Map.of("message", "OTP valid"));
    }

    // ---------------- CONFIRM RESET ----------------

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
            // Không tiết lộ email
            return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP or email"));
        }

        User user = userOpt.get();
        if (user.getResetOtp() == null || !user.getResetOtp().equalsIgnoreCase(otp) ||
                user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));
        }

        // Password policy: min 8, 1 upper, 1 lower, 1 digit, 1 special
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        if (!pattern.matcher(newPassword).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Password must be at least 8 characters, include uppercase, lowercase, digit and special character"));
        }

        // Bắt buộc password mới khác password cũ
        if (userService.checkPassword(newPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be different from old password"));
        }

        // Update password
        user.setPassword(userService.encodePassword(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userService.saveUser(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // ---------------- HELPER ----------------

    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(OTP_CHARS.charAt(random.nextInt(OTP_CHARS.length())));
        }
        return sb.toString();
    }
}


