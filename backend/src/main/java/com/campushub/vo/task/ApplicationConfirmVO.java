package com.campushub.vo.task;

import com.campushub.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApplicationConfirmVO {

    private Long orderId;
    private Long taskId;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
