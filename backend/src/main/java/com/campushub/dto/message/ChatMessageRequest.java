package com.campushub.dto.message;

import com.campushub.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotNull
    private MessageType messageType;

    private String content;

    private Long imageId;

    private Long fileId;
}
