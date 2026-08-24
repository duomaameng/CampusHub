package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publisherId;

    private TaskCategory category;

    private String title;

    private String description;

    private String campus;

    private String locationDetail;

    private RewardType rewardType;

    private BigDecimal rewardAmount;

    private LocalDateTime deadline;

    private TaskStatus status;

    private Boolean anonymous;

    private String categoryFields;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
