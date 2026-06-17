package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.service.ReportService;
import com.campushub.vo.report.ReportDetailVO;
import com.campushub.vo.report.ReportItemVO;
import com.campushub.vo.report.ReportSubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/reports")
    public ApiResponse<PageResult<ReportItemVO>> listMyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(reportService.listMyReports(page, size));
    }

    @GetMapping("/api/reports/{reportId}")
    public ApiResponse<ReportDetailVO> getReportDetail(@PathVariable Long reportId) {
        return ApiResponse.success(reportService.getReportDetail(reportId));
    }

    @PostMapping("/api/tasks/{taskId}/reports")
    public ApiResponse<ReportSubmissionVO> submit(@PathVariable Long taskId, @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.submitTaskReport(taskId, request));
    }

    @PostMapping("/api/users/{userId}/reports")
    public ApiResponse<ReportSubmissionVO> submitUserReport(@PathVariable Long userId, @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.submitUserReport(userId, request));
    }

    @PostMapping("/api/orders/{orderId}/timeout-report")
    public ApiResponse<ReportSubmissionVO> submitTimeoutOrderReport(@PathVariable Long orderId, @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.submitTimeoutOrderReport(orderId, request));
    }
}
