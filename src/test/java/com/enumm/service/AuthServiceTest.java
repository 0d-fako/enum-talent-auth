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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private AuthService authService;

    private SignUpRequest signupRequest;
    private VerifyEmailRequest verifyEmailRequest;
    private LoginRequest loginRequest;
    private User user;
    private VerificationToken verificationToken;

    @BeforeEach
    void setUp() {
        signupRequest = SignUpRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        verifyEmailRequest = VerifyEmailRequest.builder()
                .token("valid-token")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed-password")
                .status(UserStatus.PENDING_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        verificationToken = VerificationToken.builder()
                .id(1L)
                .user(user)
                .token("valid-token")
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
    }



    @Test
    void signup_withNewEmail_shouldCreatePendingUser() {
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("hashed-password");
        when(tokenGenerator.generateVerificationToken()).thenReturn("verification-token");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        AuthResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals("pending_verification", response.getStatus());
        assertNotNull(response.getToken());
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(eq(signupRequest.getEmail()), anyString());
    }

    @Test
    void signup_withExistingVerifiedEmail_shouldThrowEmailInUseException() {
        user.setStatus(UserStatus.VERIFIED);
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest));

        assertEquals(ErrorCode.EMAIL_IN_USE, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signup_withExistingUnverifiedEmail_shouldResendToken() {
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.of(user));
        when(tokenGenerator.generateVerificationToken()).thenReturn("new-token");
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest));

        assertEquals(ErrorCode.VERIFICATION_RESENT, exception.getErrorCode());
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(eq(signupRequest.getEmail()), anyString());
    }



    @Test
    void verifyEmail_withValidToken_shouldVerifyUser() {
        when(verificationTokenRepository.findByToken(verifyEmailRequest.getToken()))
                .thenReturn(Optional.of(verificationToken));
        when(userRepository.save(any(User.class))).thenReturn(user);

        AuthResponse response = authService.verifyEmail(verifyEmailRequest);

        assertNotNull(response);
        assertEquals("verified", response.getStatus());
        assertTrue(verificationToken.isUsed());
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(verificationToken);
    }

    @Test
    void verifyEmail_withInvalidToken_shouldThrowTokenInvalidException() {
        when(verificationTokenRepository.findByToken(verifyEmailRequest.getToken()))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.verifyEmail(verifyEmailRequest));

        assertEquals(ErrorCode.TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void verifyEmail_withExpiredToken_shouldThrowTokenExpiredException() {
        verificationToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        when(verificationTokenRepository.findByToken(verifyEmailRequest.getToken()))
                .thenReturn(Optional.of(verificationToken));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.verifyEmail(verifyEmailRequest));

        assertEquals(ErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void verifyEmail_withUsedToken_shouldThrowTokenAlreadyUsedException() {
        verificationToken.setUsed(true);
        when(verificationTokenRepository.findByToken(verifyEmailRequest.getToken()))
                .thenReturn(Optional.of(verificationToken));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.verifyEmail(verifyEmailRequest));

        assertEquals(ErrorCode.TOKEN_ALREADY_USED, exception.getErrorCode());
    }



    @Test
    void login_withValidCredentials_shouldReturnToken() {
        user.setStatus(UserStatus.VERIFIED);
        when(rateLimitService.isRateLimited(loginRequest.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(loginRequest.getEmail())).thenReturn("jwt-token");
        when(sessionRepository.save(any(Session.class))).thenReturn(new Session());

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(rateLimitService).resetAttempts(loginRequest.getEmail());
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void login_withUnverifiedEmail_shouldThrowEmailNotVerifiedException() {
        when(rateLimitService.isRateLimited(loginRequest.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest));

        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());
    }

    @Test
    void login_withInvalidCredentials_shouldThrowInvalidCredentialsException() {
        user.setStatus(UserStatus.VERIFIED);
        when(rateLimitService.isRateLimited(loginRequest.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verify(rateLimitService).recordAttempt(loginRequest.getEmail());
    }

    @Test
    void login_withRateLimitExceeded_shouldThrowRateLimitedException() {
        when(rateLimitService.isRateLimited(loginRequest.getEmail())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest));

        assertEquals(ErrorCode.RATE_LIMITED, exception.getErrorCode());
    }



    @Test
    void logout_withValidToken_shouldDeleteSession() {
        String token = "jwt-token";
        Session session = Session.builder()
                .id(1L)
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(sessionRepository.findByToken(token)).thenReturn(Optional.of(session));

        authService.logout(token);

        verify(sessionRepository).delete(session);
    }

    @Test
    void logout_withInvalidToken_shouldNotThrowException() {
        String token = "invalid-token";
        when(sessionRepository.findByToken(token)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.logout(token));
        verify(sessionRepository, never()).delete(any(Session.class));
    }
}