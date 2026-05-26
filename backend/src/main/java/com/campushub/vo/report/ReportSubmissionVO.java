package com.campushub.vo.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportSubmissionVO {

    private Long reportId;
    private Long taskId;
    private String reason;
    private List<Long> evidenceImageIds;
    private LocalDateTime createdAt;
}
