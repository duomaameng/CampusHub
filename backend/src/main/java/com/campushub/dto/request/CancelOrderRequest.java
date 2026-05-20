package com.campushub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelOrderRequest {

    @Size(max = 500, message = "取消原因最长 500 个字符")
    private String reason;
}
