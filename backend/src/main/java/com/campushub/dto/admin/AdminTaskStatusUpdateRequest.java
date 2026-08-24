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
    @NotNull(message = "需求状态不能为空")
    private TaskStatus status;

    @Size(max = 500, message = "处理原因不能超过 500 个字符")
    private String reason;
}
