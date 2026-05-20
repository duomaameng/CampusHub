package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.dto.report.ReportProcessRequest;
import com.campushub.entity.Report;
import com.campushub.entity.Task;
import com.campushub.enums.ReportReasonType;
import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import com.campushub.mapper.ReportMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.report.ReportSubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
/*
这个类负责：

提交举报
管理员处理举报
它已经接上的通知是：

管理员处理举报完成后
调 notificationService.createReportResultNotification(...)
它的意义是：

把举报流程和通知结果真正接起来。
*/
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    @Transactional
    public ReportSubmissionVO submitTaskReport(Long taskId, ReportCreateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        Report report = new Report();
        report.setReporterId(currentUserId);
        report.setTargetType(ReportTargetType.TASK);
        report.setTargetId(taskId);
        report.setReasonType(guessReasonType(request.getReason()));
        report.setDescription(request.getReason().trim());
        report.setStatus(ReportStatus.PENDING);
        reportMapper.insert(report);

        return new ReportSubmissionVO(
                report.getId(),
                taskId,
                request.getReason().trim(),
                request.getEvidenceImageIds() == null ? List.of() : request.getEvidenceImageIds(),
                report.getCreatedAt()
        );
    }

    @Transactional
    public void processReport(Long reportId, ReportProcessRequest request) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatus.PENDING.equals(report.getStatus()) && !ReportStatus.PROCESSING.equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED);
        }

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        report.setStatus(request.getStatus());
        report.setResult(request.getResult().trim());
        report.setProcessedBy(currentUserId);
        report.setProcessedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        notificationService.createReportResultNotification(report.getReporterId(), report.getTargetId(), request.getResult().trim());
    }

    private ReportReasonType guessReasonType(String reason) {
        String normalized = reason == null ? "" : reason.toLowerCase();
        if (normalized.contains("诈骗") || normalized.contains("fraud")) {
            return ReportReasonType.FRAUD;
        }
        if (normalized.contains("骚扰") || normalized.contains("辱骂") || normalized.contains("abuse")) {
            return ReportReasonType.ABUSE;
        }
        if (normalized.contains("广告") || normalized.contains("spam")) {
            return ReportReasonType.SPAM;
        }
        if (normalized.contains("违法") || normalized.contains("illegal")) {
            return ReportReasonType.ILLEGAL;
        }
        return ReportReasonType.OTHER;
    }
}
