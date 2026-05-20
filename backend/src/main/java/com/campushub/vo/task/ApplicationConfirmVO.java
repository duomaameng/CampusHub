package com.campushub.vo.task;

import com.campushub.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
发布者确认接单后返回：

订单 ID
任务 ID
订单状态
成单时间
它的意义是：告诉前端“已经正式成单了，接下来跳订单详情页”。
*/
@Data
@AllArgsConstructor
public class ApplicationConfirmVO {

    private Long orderId;
    private Long taskId;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
