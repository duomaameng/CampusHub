package com.campushub.dto.response;

import com.campushub.enums.MessageType;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
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
    private List<StatusLogItem> statusLogs;
    private List<MessageItem> messages;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusLogItem {
        private Long id;
        private String fromStatus;
        private String toStatus;
        private String operatorNickname;
        private String reason;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageItem {
        private Long id;
        private Long senderId;
        private String senderNickname;
        private MessageType messageType;
        private String content;
        private String imageUrl;
        private LocalDateTime createdAt;
    }
}
