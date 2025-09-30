package com.enumm.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TalentProfileResponse {

    private String email;
    private List<String> missingFields;
    private Integer completeness;
    private String transcript;
    private String statementOfPurpose;
}