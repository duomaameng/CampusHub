package com.campushub.vo.report;

import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportDetailVO {

    private Long reportId;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private ReportStatus status;
    private String result;
    private Long reporterId;
    private Long processedBy;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private List<Long> evidenceImageIds;
}
