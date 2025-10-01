package com.enumm.controller;

import com.enumm.dtos.request.TalentProfileRequest;
import com.enumm.dtos.response.TalentProfileResponse;
import com.enumm.security.JwtAuthenticationFilter;
import com.enumm.security.JwtTokenProvider;
import com.enumm.service.TalentProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TalentProfileController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}
        )
)

class TalentProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TalentProfileService talentProfileService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "test@example.com")
    void upsertProfile_withCompleteData_shouldReturnOk() throws Exception {
        TalentProfileRequest request = TalentProfileRequest.builder()
                .transcript("My transcript")
                .statementOfPurpose("My statement")
                .build();

        TalentProfileResponse response = TalentProfileResponse.builder()
                .email("test@example.com")
                .transcript("My transcript")
                .statementOfPurpose("My statement")
                .completeness(100)
                .missingFields(Collections.emptyList())
                .build();

        when(talentProfileService.upsertProfile(anyString(), any(TalentProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/profile/talent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completeness").value(100))
                .andExpect(jsonPath("$.missingFields").isEmpty());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void upsertProfile_withPartialData_shouldReturn50Percent() throws Exception {
        TalentProfileRequest request = TalentProfileRequest.builder()
                .transcript("My transcript")
                .build();

        TalentProfileResponse response = TalentProfileResponse.builder()
                .email("test@example.com")
                .transcript("My transcript")
                .completeness(50)
                .missingFields(Arrays.asList("statementOfPurpose"))
                .build();

        when(talentProfileService.upsertProfile(anyString(), any(TalentProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/profile/talent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completeness").value(50))
                .andExpect(jsonPath("$.missingFields[0]").value("statementOfPurpose"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getProfile_shouldReturnProfile() throws Exception {
        TalentProfileResponse response = TalentProfileResponse.builder()
                .email("test@example.com")
                .transcript("My transcript")
                .statementOfPurpose("My statement")
                .completeness(100)
                .missingFields(Collections.emptyList())
                .build();

        when(talentProfileService.getProfile(anyString())).thenReturn(response);

        mockMvc.perform(get("/v1/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.completeness").value(100));
    }
}