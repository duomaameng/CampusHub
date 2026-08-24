package com.campushub.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskApplyRequest {

    @NotBlank(message = "申请留言不能为空")
    private String message;
}
