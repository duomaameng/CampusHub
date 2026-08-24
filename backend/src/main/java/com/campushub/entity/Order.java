package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long publisherId;

    private Long serviceProviderId;

    private OrderStatus status;

    private String completionProofUrl;

    private String cancelReason;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
