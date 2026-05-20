package com.campushub.dto.response;

import com.campushub.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long taskId;
    private Long applicantId;
    private String applicantNickname;
    private Integer applicantCreditScore;
    private String message;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
