package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application")
public class Application {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long applicantId;

    private String message;

    private ApplicationStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
