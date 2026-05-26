package com.campushub.vo.task;

import com.campushub.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TaskApplyVO {

    private Long applicationId;
    private Long taskId;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
