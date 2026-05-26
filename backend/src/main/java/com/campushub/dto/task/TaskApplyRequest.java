package com.campushub.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskApplyRequest {

    @NotBlank
    private String message;
}
