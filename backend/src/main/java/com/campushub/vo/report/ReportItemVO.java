package com.campushub.vo.report;

import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import com.campushub.enums.ReportReasonType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportItemVO {

    private Long reportId;
    private ReportTargetType targetType;
    private Long targetId;
    private Long relatedOrderId;
    private ReportReasonType reasonType;
    private String reason;
    private ReportStatus status;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
