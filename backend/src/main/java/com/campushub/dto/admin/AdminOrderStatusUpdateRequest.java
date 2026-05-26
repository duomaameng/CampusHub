package com.campushub.dto.admin;

import com.campushub.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminOrderStatusUpdateRequest {

    /**
     * Admin order action is intentionally narrowed to:
     * DISPUTE     - freeze an active order
     * IN_PROGRESS - restore a disputed order
     */
    @NotNull
    private OrderStatus status;

    @Size(max = 500)
    private String reason;
}
