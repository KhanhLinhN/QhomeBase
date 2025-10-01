package com.qhomebaseapp.loginservice.controller;

import com.qhomebaseapp.loginservice.dto.ResetPasswordRequest;
import com.qhomebaseapp.loginservice.entity.User;
import com.qhomebaseapp.loginservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userService.getUserByEmail(loginRequest.getEmail());

        if (user.isPresent()) {
            User existing = user.get();

            if (userService.checkPassword(loginRequest.getPassword(), existing.getPassword())) {
                return ResponseEntity.ok(existing);
            }
        }

        return ResponseEntity.status(401).body("Invalid email or password");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // logout đơn giản (sau này có JWT thì revoke token)
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
        Optional<User> user = userService.getUserByEmail(resetRequest.getEmail());

        if (user.isPresent()) {
            User existing = user.get();
            String hashedPassword = userService.encodePassword(resetRequest.getNewPassword());
            existing.setPassword(hashedPassword);
            userService.saveUser(existing);
            return ResponseEntity.ok("Password updated");
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }
}
