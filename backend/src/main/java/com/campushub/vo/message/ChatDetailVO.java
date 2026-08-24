package com.campushub.vo.message;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatDetailVO {
    private Long conversationId;
    private ChatUserVO participant;
    private List<ChatMessageVO> messages;
}
