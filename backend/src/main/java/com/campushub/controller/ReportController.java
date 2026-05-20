package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.dto.report.ReportProcessRequest;
import com.campushub.service.ReportService;
import com.campushub.vo.report.ReportSubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/*
这个类暴露了：

POST /api/tasks/{taskId}/reports
PATCH /api/admin/reports/{reportId}
它的意义是：

前端举报任务，以及后台处理举报，都有了入口。
*/
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/api/tasks/{taskId}/reports")
    public ApiResponse<ReportSubmissionVO> submit(@PathVariable Long taskId, @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.submitTaskReport(taskId, request));
    }

    @PatchMapping("/api/admin/reports/{reportId}")
    public ApiResponse<Void> process(@PathVariable Long reportId, @Valid @RequestBody ReportProcessRequest request) {
        reportService.processReport(reportId, request);
        return ApiResponse.success();
    }
}
