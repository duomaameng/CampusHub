package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.ReportReasonType;
import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;

    private ReportTargetType targetType;

    private Long targetId;

    private ReportReasonType reasonType;

    private String description;

    private ReportStatus status;

    private String result;

    private Long processedBy;

    private LocalDateTime processedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
