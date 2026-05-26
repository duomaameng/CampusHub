package com.campushub.vo.admin;

import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportItemVO {

    private Long id;
    private Long reporterId;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
