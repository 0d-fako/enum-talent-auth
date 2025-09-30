package com.enumm.service;

import com.enumm.dtos.request.TalentProfileRequest;
import com.enumm.dtos.response.TalentProfileResponse;
import com.enumm.enums.ErrorCode;
import com.enumm.exception.BusinessException;
import com.enumm.model.TalentProfile;
import com.enumm.model.User;
import com.enumm.repository.TalentProfileRepository;
import com.enumm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TalentProfileService {

    private final TalentProfileRepository talentProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public TalentProfileResponse upsertProfile(String email, TalentProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_AUTHENTICATED,
                        HttpStatus.UNAUTHORIZED,
                        "User not authenticated. Please log in."
                ));

        TalentProfile profile = talentProfileRepository.findByUser(user)
                .orElse(TalentProfile.builder()
                        .user(user)
                        .build());


        if (request.getTranscript() != null) {
            profile.setTranscript(request.getTranscript());
        }
        if (request.getStatementOfPurpose() != null) {
            profile.setStatementOfPurpose(request.getStatementOfPurpose());
        }

        TalentProfile savedProfile = talentProfileRepository.save(profile);

        return buildProfileResponse(user, savedProfile);
    }

    @Transactional(readOnly = true)
    public TalentProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_AUTHENTICATED,
                        HttpStatus.UNAUTHORIZED,
                        "User not authenticated. Please log in."
                ));

        TalentProfile profile = talentProfileRepository.findByUser(user)
                .orElse(null);

        return buildProfileResponse(user, profile);
    }

    private TalentProfileResponse buildProfileResponse(User user, TalentProfile profile) {
        String transcript = profile != null ? profile.getTranscript() : null;
        String statementOfPurpose = profile != null ? profile.getStatementOfPurpose() : null;

        int completeness = calculateCompleteness(transcript, statementOfPurpose);
        List<String> missingFields = getMissingFields(transcript, statementOfPurpose);

        return TalentProfileResponse.builder()
                .email(user.getEmail())
                .transcript(transcript)
                .statementOfPurpose(statementOfPurpose)
                .completeness(completeness)
                .missingFields(missingFields)
                .build();
    }

    private int calculateCompleteness(String transcript, String statementOfPurpose) {
        int fieldCount = 0;

        if (isFieldPresent(transcript)) fieldCount++;
        if (isFieldPresent(statementOfPurpose)) fieldCount++;

        return (fieldCount * 100) / 2;
    }

    private List<String> getMissingFields(String transcript, String statementOfPurpose) {
        List<String> missing = new ArrayList<>();

        if (!isFieldPresent(transcript)) {
            missing.add("transcript");
        }
        if (!isFieldPresent(statementOfPurpose)) {
            missing.add("statementOfPurpose");
        }

        return missing;
    }

    private boolean isFieldPresent(String field) {
        return field != null && !field.trim().isEmpty();
    }
}