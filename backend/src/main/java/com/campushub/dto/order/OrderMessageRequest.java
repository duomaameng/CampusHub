package com.campushub.dto.order;

import com.campushub.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderMessageRequest {

    @NotNull
    private MessageType messageType;

    private String content;

    private Long imageId;
}
