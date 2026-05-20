package com.campushub.vo.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
/*
举报提交成功后返回：

举报 ID
任务 ID
举报原因
证据图片 ID
创建时间
它的意义是：告诉前端“举报已经提交成功”。
*/
@Data
@AllArgsConstructor
public class ReportSubmissionVO {

    private Long reportId;
    private Long taskId;
    private String reason;
    private List<Long> evidenceImageIds;
    private LocalDateTime createdAt;
}
