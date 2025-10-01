package com.enumm.controller;

import com.enumm.dtos.request.LoginRequest;
import com.enumm.dtos.request.SignUpRequest;
import com.enumm.dtos.request.VerifyEmailRequest;
import com.enumm.dtos.response.AuthResponse;
import com.enumm.enums.ErrorCode;
import com.enumm.exception.BusinessException;
//import com.enumm.security.JwtAuthenticationFilter;
//import com.enumm.security.JwtTokenProvider;
import com.enumm.security.JwtAuthenticationFilter;
import com.enumm.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

//    @MockBean
//    private JwtTokenProvider jwtTokenProvider;
//
//    @MockBean
//    private JwtAuthenticationFilter jwtAuthenticationFilter;
//

    @Test
    @WithMockUser
    void signup_withValidRequest_shouldReturnCreated() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .message("Account created successfully")
                .token("verification-token")
                .status("pending_verification")
                .build();

        when(authService.signup(any(SignUpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.token").value("verification-token"))
                .andExpect(jsonPath("$.status").value("pending_verification"));
    }

    @Test
    @WithMockUser
    void signup_withInvalidEmail_shouldReturnUnprocessableEntity() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    void signup_withShortPassword_shouldReturnUnprocessableEntity() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .email("test@example.com")
                .password("short")
                .build();

        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    void signup_withExistingEmail_shouldReturnConflict() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .email("existing@example.com")
                .password("password123")
                .build();

        when(authService.signup(any(SignUpRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.EMAIL_IN_USE,
                        HttpStatus.CONFLICT,
                        "This email is already registered"
                ));

        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_IN_USE"));
    }


    @Test
    @WithMockUser
    void verifyEmail_withValidToken_shouldReturnOk() throws Exception {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token("valid-token")
                .build();

        AuthResponse response = AuthResponse.builder()
                .message("Email verified successfully")
                .status("verified")
                .build();

        when(authService.verifyEmail(any(VerifyEmailRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/verify-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"))
                .andExpect(jsonPath("$.status").value("verified"));
    }

    @Test
    @WithMockUser
    void verifyEmail_withExpiredToken_shouldReturnBadRequest() throws Exception {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token("expired-token")
                .build();

        when(authService.verifyEmail(any(VerifyEmailRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.TOKEN_EXPIRED,
                        HttpStatus.BAD_REQUEST,
                        "Token has expired"
                ));

        mockMvc.perform(post("/v1/auth/verify-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
    }



    @Test
    @WithMockUser
    void login_withValidCredentials_shouldReturnOk() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .message("Login successful")
                .token("jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @WithMockUser
    void login_withUnverifiedEmail_shouldReturnForbidden() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("unverified@example.com")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.EMAIL_NOT_VERIFIED,
                        HttpStatus.FORBIDDEN,
                        "Please verify your email"
                ));

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    @WithMockUser
    void login_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.INVALID_CREDENTIALS,
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"
                ));

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }



    @Test
    @WithMockUser
    void logout_withValidToken_shouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isNoContent());
    }
}