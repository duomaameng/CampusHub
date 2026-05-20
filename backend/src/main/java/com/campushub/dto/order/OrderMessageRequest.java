package com.campushub.dto.order;

import com.campushub.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/*
这个类负责接订单聊天消息，支持两种：

文字消息
图片消息
它里面有：

messageType
content
imageId
它的意义是：订单聊天统一走这一套输入结构。
*/
@Data
public class OrderMessageRequest {

    @NotNull
    private MessageType messageType;

    private String content;

    private Long imageId;
}
