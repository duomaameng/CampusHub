package com.campushub.vo.admin;

import com.campushub.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderItemVO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long publisherId;
    private Long serviceProviderId;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
