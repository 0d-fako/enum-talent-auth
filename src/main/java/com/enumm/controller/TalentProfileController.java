package com.enumm.controller;

import com.enumm.dtos.request.TalentProfileRequest;
import com.enumm.dtos.response.TalentProfileResponse;
import com.enumm.service.TalentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Talent Profile", description = "Talent profile management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class TalentProfileController {

    private final TalentProfileService talentProfileService;

    @PostMapping("/v1/profile/talent")
    @Operation(summary = "Create/Update talent profile", description = "Create or update talent profile")
    public ResponseEntity<TalentProfileResponse> upsertProfile(
            @RequestBody TalentProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        TalentProfileResponse response = talentProfileService.upsertProfile(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/profile/me")
    @Operation(summary = "Get my profile", description = "Get current user's profile")
    public ResponseEntity<TalentProfileResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        TalentProfileResponse response = talentProfileService.getProfile(email);
        return ResponseEntity.ok(response);
    }
}