package com.example.DACN.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a 6-digit OTP code
     * 
     * @return 6-digit OTP as String
     */
    public String generateOtp() {
        int otp = random.nextInt(900000) + 100000; // Range: 100000-999999
        String otpCode = String.valueOf(otp);
        log.debug("Generated OTP: {}", otpCode);
        return otpCode;
    }
}
