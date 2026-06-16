package com.campushub.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCancelRequest {

    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
