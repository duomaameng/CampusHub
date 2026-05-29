package com.campushub.dto.admin;

import com.campushub.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminOrderStatusUpdateRequest {

    /**
     * Admin order action is intentionally narrowed to:
     * DISPUTE - freeze an active order
     * others  - when the current order is DISPUTE, restore it to the exact
     *           original status before the dispute (for example IN_PROGRESS,
     *           PENDING_CONFIRM, or PENDING_COMPLETION)
     */
    @NotNull
    private OrderStatus status;

    @Size(max = 500)
    private String reason;
}
