package com.campushub.vo.order;

import com.campushub.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderMessageVO {

    private Long id;
    private Long orderId;
    private Long senderId;
    private String senderNickname;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
}
