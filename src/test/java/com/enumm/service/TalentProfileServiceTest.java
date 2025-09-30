package com.enumm.service;

import com.enumm.dtos.request.TalentProfileRequest;
import com.enumm.dtos.response.TalentProfileResponse;
import com.enumm.enums.ErrorCode;
import com.enumm.exception.BusinessException;
import com.enumm.model.TalentProfile;
import com.enumm.model.User;
import com.enumm.repository.TalentProfileRepository;
import com.enumm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TalentProfileServiceTest {

    @Mock
    private TalentProfileRepository talentProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TalentProfileService talentProfileService;

    private User user;
    private TalentProfile talentProfile;
    private TalentProfileRequest profileRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        talentProfile = TalentProfile.builder()
                .id(1L)
                .user(user)
                .transcript("My transcript")
                .statementOfPurpose("My statement")
                .build();

        profileRequest = TalentProfileRequest.builder()
                .transcript("Updated transcript")
                .statementOfPurpose("Updated statement")
                .build();
    }


    @Test
    void upsertProfile_withBothFields_shouldCreate100PercentComplete() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(talentProfileRepository.save(any(TalentProfile.class))).thenReturn(talentProfile);

        TalentProfileResponse response = talentProfileService.upsertProfile(user.getEmail(), profileRequest);

        assertNotNull(response);
        assertEquals(100, response.getCompleteness());
        assertTrue(response.getMissingFields().isEmpty());
        verify(talentProfileRepository).save(any(TalentProfile.class));
    }

    @Test
    void upsertProfile_withOnlyTranscript_shouldCreate50PercentComplete() {
        profileRequest.setStatementOfPurpose(null);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(talentProfileRepository.save(any(TalentProfile.class))).thenAnswer(invocation -> {
            TalentProfile saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TalentProfileResponse response = talentProfileService.upsertProfile(user.getEmail(), profileRequest);

        assertEquals(50, response.getCompleteness());
        assertEquals(1, response.getMissingFields().size());
        assertTrue(response.getMissingFields().contains("statementOfPurpose"));
    }

    @Test
    void upsertProfile_withOnlyStatementOfPurpose_shouldCreate50PercentComplete() {
        profileRequest.setTranscript(null);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(talentProfileRepository.save(any(TalentProfile.class))).thenAnswer(invocation -> {
            TalentProfile saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TalentProfileResponse response = talentProfileService.upsertProfile(user.getEmail(), profileRequest);

        assertEquals(50, response.getCompleteness());
        assertEquals(1, response.getMissingFields().size());
        assertTrue(response.getMissingFields().contains("transcript"));
    }

    @Test
    void upsertProfile_withNoFields_shouldCreate0PercentComplete() {
        profileRequest.setTranscript(null);
        profileRequest.setStatementOfPurpose(null);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(talentProfileRepository.save(any(TalentProfile.class))).thenAnswer(invocation -> {
            TalentProfile saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TalentProfileResponse response = talentProfileService.upsertProfile(user.getEmail(), profileRequest);

        assertEquals(0, response.getCompleteness());
        assertEquals(2, response.getMissingFields().size());
        assertTrue(response.getMissingFields().containsAll(Arrays.asList("transcript", "statementOfPurpose")));
    }

    @Test
    void upsertProfile_withExistingProfile_shouldUpdate() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.of(talentProfile));
        when(talentProfileRepository.save(any(TalentProfile.class))).thenReturn(talentProfile);

        TalentProfileResponse response = talentProfileService.upsertProfile(user.getEmail(), profileRequest);

        assertNotNull(response);
        verify(talentProfileRepository).save(talentProfile);
        assertEquals("Updated transcript", talentProfile.getTranscript());
        assertEquals("Updated statement", talentProfile.getStatementOfPurpose());
    }

    @Test
    void upsertProfile_withNonExistentUser_shouldThrowException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> talentProfileService.upsertProfile(user.getEmail(), profileRequest));

        assertEquals(ErrorCode.NOT_AUTHENTICATED, exception.getErrorCode());
    }


    @Test
    void getProfile_withExistingProfile_shouldReturnProfile() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.of(talentProfile));

        TalentProfileResponse response = talentProfileService.getProfile(user.getEmail());

        assertNotNull(response);
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals("My transcript", response.getTranscript());
        assertEquals("My statement", response.getStatementOfPurpose());
        assertEquals(100, response.getCompleteness());
        assertTrue(response.getMissingFields().isEmpty());
    }

    @Test
    void getProfile_withNoProfile_shouldReturnEmptyProfile() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        TalentProfileResponse response = talentProfileService.getProfile(user.getEmail());

        assertNotNull(response);
        assertEquals(user.getEmail(), response.getEmail());
        assertNull(response.getTranscript());
        assertNull(response.getStatementOfPurpose());
        assertEquals(0, response.getCompleteness());
        assertEquals(2, response.getMissingFields().size());
    }

    @Test
    void getProfile_withPartialProfile_shouldCalculateCorrectCompleteness() {
        talentProfile.setStatementOfPurpose(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.of(talentProfile));

        TalentProfileResponse response = talentProfileService.getProfile(user.getEmail());

        assertEquals(50, response.getCompleteness());
        assertEquals(1, response.getMissingFields().size());
        assertTrue(response.getMissingFields().contains("statementOfPurpose"));
    }

    @Test
    void getProfile_withNonExistentUser_shouldThrowException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> talentProfileService.getProfile(user.getEmail()));

        assertEquals(ErrorCode.NOT_AUTHENTICATED, exception.getErrorCode());
    }


    @Test
    void calculateCompleteness_withEmptyStrings_shouldTreatAsNull() {
        talentProfile.setTranscript("");
        talentProfile.setStatementOfPurpose("   ");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(talentProfileRepository.findByUser(user)).thenReturn(Optional.of(talentProfile));

        TalentProfileResponse response = talentProfileService.getProfile(user.getEmail());

        assertEquals(0, response.getCompleteness());
        assertEquals(2, response.getMissingFields().size());
    }
}