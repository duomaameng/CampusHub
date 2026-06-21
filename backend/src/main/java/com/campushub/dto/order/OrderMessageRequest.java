package com.campushub.dto.order;

import com.campushub.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderMessageRequest {

    @NotNull(message = "消息类型不能为空")
    private MessageType messageType;

    private String content;

    private Long imageId;

    private Long fileId;
}
