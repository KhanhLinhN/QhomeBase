package com.qhomebaseapp.service.user;

import com.qhomebaseapp.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> getAllUsers();

    Optional<User> getUserByEmail(String email);

    boolean existsByEmail(String email);

    User saveUser(User user);

    boolean checkPassword(String rawPassword, String encodedPassword);

    String encodePassword(String rawPassword);

    String generateAndSaveOtp(User user);

    void sendOtpViaSms(String phoneNumber, String otp);

    boolean verifyOtp(User user, String otp);
}