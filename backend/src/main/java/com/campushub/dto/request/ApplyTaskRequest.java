package com.campushub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyTaskRequest {

    @NotBlank(message = "申请消息不能为空")
    @Size(min = 1, max = 500, message = "申请消息长度 1-500 个字符")
    private String message;
}
