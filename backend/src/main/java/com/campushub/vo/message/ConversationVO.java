package com.campushub.vo.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {
    private Long id;
    private ChatUserVO participant;
    private String lastMessageText;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
