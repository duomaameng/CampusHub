package com.campushub.vo.order;

import com.campushub.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
这是订单状态日志的展示对象。
里面有：

从什么状态变到什么状态
谁操作的
为什么变
什么时间变的
它的意义是：前端订单详情里的状态时间线靠它显示。
*/
@Data
@AllArgsConstructor
public class OrderStatusLogVO {

    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String operatorNickname;
    private String reason;
    private LocalDateTime createdAt;
}
