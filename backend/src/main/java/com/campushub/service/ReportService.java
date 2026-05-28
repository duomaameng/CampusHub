package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.entity.FileRecord;
import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.dto.report.ReportProcessRequest;
import com.campushub.entity.Report;
import com.campushub.entity.ReportEvidence;
import com.campushub.entity.Task;
import com.campushub.enums.ReportReasonType;
import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.ReportEvidenceMapper;
import com.campushub.mapper.ReportMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.report.ReportDetailVO;
import com.campushub.vo.report.ReportItemVO;
import com.campushub.vo.report.ReportSubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReportMapper reportMapper;
    private final ReportEvidenceMapper reportEvidenceMapper;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final FileService fileService;

    public PageResult<ReportItemVO> listMyReports(int page, int size) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        Page<Report> result = reportMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, currentUserId)
                        .orderByDesc(Report::getCreatedAt)
        );

        List<ReportItemVO> records = result.getRecords().stream()
                .map(this::toReportItemVO)
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    public PageResult<ReportItemVO> listAdminReports(int page, int size, ReportStatus status) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getCreatedAt);
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }

        Page<Report> result = reportMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<ReportItemVO> records = result.getRecords().stream()
                .map(this::toReportItemVO)
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    public ReportDetailVO getReportDetail(Long reportId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (!currentUserId.equals(report.getReporterId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You are not allowed to view this report");
        }
        return toReportDetailVO(report);
    }

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

        List<Long> evidenceImageIds = request.getEvidenceImageIds() == null ? List.of() : request.getEvidenceImageIds();
        for (Long imageId : evidenceImageIds) {
            FileRecord fileRecord = fileService.requireOwnedFile(imageId, UploadBusinessType.REPORT_EVIDENCE);
            ReportEvidence evidence = new ReportEvidence();
            evidence.setReportId(report.getId());
            evidence.setFileRecordId(fileRecord.getId());
            reportEvidenceMapper.insert(evidence);
        }

        return new ReportSubmissionVO(
                report.getId(),
                taskId,
                request.getReason().trim(),
                evidenceImageIds,
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
        if (!ReportStatus.RESOLVED.equals(request.getStatus()) && !ReportStatus.REJECTED.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Processed report status must be RESOLVED or REJECTED");
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

    private int normalizePage(int page) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Size must be greater than or equal to 1");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private ReportItemVO toReportItemVO(Report report) {
        return new ReportItemVO(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getDescription(),
                report.getStatus(),
                report.getResult(),
                report.getCreatedAt(),
                report.getProcessedAt()
        );
    }

    private ReportDetailVO toReportDetailVO(Report report) {
        return new ReportDetailVO(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getDescription(),
                report.getStatus(),
                report.getResult(),
                report.getReporterId(),
                report.getProcessedBy(),
                report.getCreatedAt(),
                report.getProcessedAt(),
                loadEvidenceImageIds(report.getId())
        );
    }

    private List<Long> loadEvidenceImageIds(Long reportId) {
        return reportEvidenceMapper.selectList(
                        new LambdaQueryWrapper<ReportEvidence>()
                                .eq(ReportEvidence::getReportId, reportId)
                                .orderByAsc(ReportEvidence::getId)
                )
                .stream()
                .map(ReportEvidence::getFileRecordId)
                .toList();
    }
}
