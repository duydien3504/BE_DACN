package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.constant.UserStatus;
import com.example.DACN.dto.request.ChangePasswordRequest;
import com.example.DACN.dto.request.ForgotPasswordRequest;
import com.example.DACN.dto.request.LoginRequest;
import com.example.DACN.dto.request.RegisterRequest;
import com.example.DACN.dto.request.ResetPasswordRequest;
import com.example.DACN.dto.response.ChangePasswordResponse;
import com.example.DACN.dto.response.ForgotPasswordResponse;
import com.example.DACN.dto.response.LoginResponse;
import com.example.DACN.dto.response.RegisterResponse;
import com.example.DACN.dto.response.ResetPasswordResponse;
import com.example.DACN.entity.PasswordResetToken;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.exception.AuthenticationFailedException;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.exception.InvalidOtpException;
import com.example.DACN.mapper.UserMapper;
import com.example.DACN.repository.PasswordResetTokenRepository;
import com.example.DACN.repository.RoleRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Starting registration for email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", request.getEmail());
            throw new DuplicateResourceException("Email already exists");
        }

        // Check if phone number already exists
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            log.warn("Registration failed: Phone number already exists - {}", request.getPhoneNumber());
            throw new DuplicateResourceException("Phone number already exists");
        }

        // Get Customer role
        Role customerRole = roleRepository.findByRoleName(RoleConstants.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Customer role not found. Please initialize roles."));

        // Map request to entity
        User user = userMapper.toEntity(request);

        // Set additional fields
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsEmailVerified(false);
        user.setHasDeleted(false);

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        // Map to response
        return userMapper.toRegisterResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", request.getEmail());
                    return new AuthenticationFailedException("Invalid email or password");
                });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for email - {}", request.getEmail());
            throw new AuthenticationFailedException("Invalid email or password");
        }

        // Check if user is banned
        if (UserStatus.BANNED.equals(user.getStatus())) {
            log.warn("Login failed: User is banned - {}", request.getEmail());
            throw new AuthenticationFailedException("Account has been banned");
        }

        // Check if user is deleted
        if (user.getHasDeleted()) {
            log.warn("Login failed: User account deleted - {}", request.getEmail());
            throw new AuthenticationFailedException("Account not found");
        }

        // Generate JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId().toString());
        claims.put("roleId", user.getRole().getRoleId());
        claims.put("role", user.getRole().getRoleName());

        String token = jwtUtil.generateToken(user.getEmail(), claims);

        log.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole().getRoleName());

        return new LoginResponse(token, user.getRole().getRoleName());
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Forgot password failed: User not found - {}", request.getEmail());
                    // Don't reveal if email exists for security
                    return new RuntimeException("If this email exists, a reset code will be sent");
                });

        // Generate OTP
        String otpCode = otpService.generateOtp();

        // Create password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getUserId());
        resetToken.setOtpCode(otpCode);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15)); // 15 minutes expiration
        resetToken.setIsUsed(false);

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token created for user: {}", user.getEmail());

        // Send email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), otpCode);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send email. Please try again later.");
        }

        return new ForgotPasswordResponse("Password reset code sent to your email");
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Reset password failed: User not found - {}", request.getEmail());
                    return new InvalidOtpException("Invalid OTP or email");
                });

        // Verify OTP
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                        user.getUserId(),
                        request.getOtp(),
                        LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Reset password failed: Invalid or expired OTP for user - {}", request.getEmail());
                    return new InvalidOtpException("Invalid or expired OTP");
                });

        // Hash new password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        // Update password
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        // Mark token as used
        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());

        return new ResetPasswordResponse("Password reset successfully");
    }

    @Transactional
    public ChangePasswordResponse changePassword(String email, ChangePasswordRequest request) {
        log.info("Change password request for user: {}", email);

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Change password failed: User not found - {}", email);
                    return new AuthenticationFailedException("User not found");
                });

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            log.warn("Change password failed: Incorrect old password for user - {}", email);
            throw new AuthenticationFailedException("Old password is incorrect");
        }

        // Hash new password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        // Update password
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());

        return new ChangePasswordResponse("Password changed successfully");
    }
}
