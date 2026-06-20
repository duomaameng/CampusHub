package com.campushub.vo.order;

import com.campushub.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderItemVO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long publisherId;
    private String publisherNickname;
    private String publisherAvatarUrl;
    private Long serviceProviderId;
    private String serviceProviderNickname;
    private String serviceProviderAvatarUrl;
    private OrderStatus status;
    private String cancelReason;
    private LocalDateTime createdAt;
    private String taskImageUrl;
}
