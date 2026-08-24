package com.campushub.dto.admin;

import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import lombok.Data;

@Data
public class AdminReportQueryRequest {

    private ReportStatus status;
    private ReportTargetType targetType;
    private String keyword;
}
