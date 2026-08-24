package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long senderId;

    private MessageType messageType;

    private String content;

    private String imageUrl;

    private Long fileId;

    private Boolean isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
