package com.campushub.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCancelRequest {

    @NotBlank
    private String reason;
}
