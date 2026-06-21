package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_file")
public class TaskFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long fileRecordId;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
