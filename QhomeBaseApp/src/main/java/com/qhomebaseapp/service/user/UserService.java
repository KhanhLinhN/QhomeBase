package com.qhomebaseapp.service.user;


import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User saveUser(User user) {
        // Hash password nếu chưa được mã hóa
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Sinh OTP và lưu vào user (dùng cho cả email & sms/zalo)
     */
    public String generateAndSaveOtp(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        userRepository.save(user);
        return otp;
    }

    /**
     * Gửi OTP qua SMS/Zalo (mock để bạn tích hợp thật sau)
     */
    public void sendOtpViaSms(String phoneNumber, String otp) {
        // TODO: Tích hợp với Twilio, Viettel SMS Gateway, hoặc Zalo ZNS
        System.out.println("📲 Gửi OTP " + otp + " đến số: " + phoneNumber);
    }

    /**
     * Check OTP hợp lệ (dùng chung cho email/sms)
     */
    public boolean verifyOtp(User user, String otp) {
        if (user.getResetOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }
        if (!otp.equals(user.getResetOtp())) {
            return false;
        }
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
}
