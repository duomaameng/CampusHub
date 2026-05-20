package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_log")
public class CreditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer changeAmount;

    private Integer scoreBefore;

    private Integer scoreAfter;

    private String reason;

    private Long relatedOrderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
