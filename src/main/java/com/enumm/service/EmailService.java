package com.enumm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendVerificationEmail(String email, String token) {
        // Mock implementation
        log.info("==============================================");
        log.info("VERIFICATION EMAIL (Mock)");
        log.info("To: {}", email);
        log.info("Verification Token: {}", token);
        log.info("Verification Link: http://localhost:8080/v1/auth/verify-email?token={}", token);
        log.info("==============================================");
    }
}