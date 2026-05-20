package com.campushub.vo.order;

import com.campushub.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
/*
这是订单列表页用的基础对象。
里面有：

订单 ID
任务标题
发布者
服务方
订单状态
创建时间
它的意义是：“我的订单”页面主要先用它展示列表。
*/
@Data
public class OrderItemVO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long publisherId;
    private String publisherNickname;
    private Long serviceProviderId;
    private String serviceProviderNickname;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
