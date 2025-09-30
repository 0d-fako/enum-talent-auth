package com.enumm.service;

import com.enumm.dtos.request.LoginRequest;
import com.enumm.dtos.request.SignUpRequest;
import com.enumm.dtos.request.VerifyEmailRequest;
import com.enumm.dtos.response.AuthResponse;
import com.enumm.enums.ErrorCode;
import com.enumm.enums.UserStatus;
import com.enumm.exception.BusinessException;
import com.enumm.model.Session;
import com.enumm.model.User;
import com.enumm.model.VerificationToken;
import com.enumm.repository.SessionRepository;
import com.enumm.repository.UserRepository;
import com.enumm.repository.VerificationTokenRepository;
import com.enumm.security.JwtTokenProvider;
import com.enumm.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    @Value("${verification.token.expiration}")
    private long verificationTokenExpiration;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.getStatus() == UserStatus.VERIFIED) {
                throw new BusinessException(
                        ErrorCode.EMAIL_IN_USE,
                        HttpStatus.CONFLICT,
                        "This email is already registered. Please log in or reset your password."
                );
            }


            String newToken = tokenGenerator.generateVerificationToken();
            VerificationToken verificationToken = VerificationToken.builder()
                    .user(user)
                    .token(newToken)
                    .expiryDate(LocalDateTime.now().plusNanos(verificationTokenExpiration * 1_000_000))
                    .used(false)
                    .build();

            verificationTokenRepository.save(verificationToken);
            emailService.sendVerificationEmail(user.getEmail(), newToken);

            throw new BusinessException(
                    ErrorCode.VERIFICATION_RESENT,
                    HttpStatus.ACCEPTED,
                    "A new verification link has been sent to your email. Please check your inbox."
            );
        }


        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        User savedUser = userRepository.save(newUser);


        String token = tokenGenerator.generateVerificationToken();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(savedUser)
                .token(token)
                .expiryDate(LocalDateTime.now().plusNanos(verificationTokenExpiration * 1_000_000))
                .used(false)
                .build();

        verificationTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return AuthResponse.builder()
                .message("Account created successfully. Please check your email to verify your account.")
                .token(token)
                .status("pending_verification")
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOKEN_INVALID,
                        HttpStatus.BAD_REQUEST,
                        "Invalid verification token. Please request a new verification email."
                ));

        if (verificationToken.isUsed()) {
            throw new BusinessException(
                    ErrorCode.TOKEN_ALREADY_USED,
                    HttpStatus.BAD_REQUEST,
                    "This verification link has already been used. Please log in."
            );
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    ErrorCode.TOKEN_EXPIRED,
                    HttpStatus.BAD_REQUEST,
                    "Verification link has expired. Please request a new one."
            );
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.VERIFIED);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        return AuthResponse.builder()
                .message("Email verified successfully! You can now log in.")
                .status("verified")
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (rateLimitService.isRateLimited(request.getEmail())) {
            throw new BusinessException(
                    ErrorCode.RATE_LIMITED,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again in 15 minutes."
            );
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    rateLimitService.recordAttempt(request.getEmail());
                    return new BusinessException(
                            ErrorCode.INVALID_CREDENTIALS,
                            HttpStatus.UNAUTHORIZED,
                            "Invalid email or password. Please check your details and try again."
                    );
                });

        if (user.getStatus() != UserStatus.VERIFIED) {
            throw new BusinessException(
                    ErrorCode.EMAIL_NOT_VERIFIED,
                    HttpStatus.FORBIDDEN,
                    "Please verify your email before logging in. Check your inbox for the verification link."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimitService.recordAttempt(request.getEmail());
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password. Please check your details and try again."
            );
        }


        rateLimitService.resetAttempts(request.getEmail());


        String jwtToken = jwtTokenProvider.generateToken(user.getEmail());


        Session session = Session.builder()
                .user(user)
                .token(jwtToken)
                .expiryDate(LocalDateTime.now().plusNanos(jwtExpiration * 1_000_000))
                .build();

        sessionRepository.save(session);

        return AuthResponse.builder()
                .message("Login successful!")
                .token(jwtToken)
                .build();
    }

    @Transactional
    public void logout(String token) {
        Optional<Session> session = sessionRepository.findByToken(token);
        session.ifPresent(sessionRepository::delete);
    }
}