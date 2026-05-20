package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_message")
public class OrderMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long senderId;

    private MessageType messageType;

    private String content;

    private String imageUrl;

    private Boolean isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
