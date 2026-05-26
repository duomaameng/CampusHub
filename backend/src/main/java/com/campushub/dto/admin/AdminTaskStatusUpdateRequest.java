package com.campushub.dto.admin;

import com.campushub.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminTaskStatusUpdateRequest {

    /**
     * Admin task action is intentionally narrowed to:
     * OPEN      - restore a previously taken-down task
     * CANCELLED - take down an open task
     */
    @NotNull
    private TaskStatus status;

    @Size(max = 500)
    private String reason;
}
