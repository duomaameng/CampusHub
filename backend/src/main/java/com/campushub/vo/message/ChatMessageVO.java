package com.campushub.vo.message;

import com.campushub.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageVO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private Boolean read;
    private LocalDateTime createdAt;
}
