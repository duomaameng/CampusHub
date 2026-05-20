package com.campushub.dto.response;

import com.campushub.enums.OrderStatus;
import com.campushub.enums.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private String taskDescription;
    private String campus;
    private RewardType rewardType;
    private Long publisherId;
    private String publisherNickname;
    private Long serviceProviderId;
    private String serviceProviderNickname;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
