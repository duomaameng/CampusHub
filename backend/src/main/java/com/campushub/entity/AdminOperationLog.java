package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_operation_log")
public class AdminOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    private String operationType;

    private String targetType;

    private Long targetId;

    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
