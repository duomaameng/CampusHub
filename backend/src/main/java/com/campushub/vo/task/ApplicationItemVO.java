package com.campushub.vo.task;

import com.campushub.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationItemVO {

    private Long id;
    private Long taskId;
    private Long applicantId;
    private String applicantNickname;
    private String applicantAvatarUrl;
    private Integer applicantCreditScore;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
