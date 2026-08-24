package com.campushub.vo.order;

import com.campushub.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderStatusLogVO {

    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private Long operatorId;
    private String operatorNickname;
    private String reason;
    private LocalDateTime createdAt;
}
