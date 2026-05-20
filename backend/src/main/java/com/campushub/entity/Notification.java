package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private NotificationType type;

    private String title;

    private String content;

    private Boolean isRead;

    private Boolean isDeleted;

    private Long relatedOrderId;

    private Long relatedTaskId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
