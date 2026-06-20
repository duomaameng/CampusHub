package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.entity.CreditLog;
import com.campushub.entity.FileRecord;
import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.dto.report.ReportProcessRequest;
import com.campushub.entity.Order;
import com.campushub.entity.Report;
import com.campushub.entity.ReportEvidence;
import com.campushub.entity.Task;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.ReportReasonType;
import com.campushub.enums.ReportStatus;
import com.campushub.enums.ReportTargetType;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.ReportEvidenceMapper;
import com.campushub.mapper.ReportMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
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
    private static final int DEFAULT_CREDIT_SCORE = 100;
    private static final int MIN_CREDIT_SCORE = 0;
    private static final int MAX_CREDIT_SCORE = 100;
    private static final int DEFAULT_TIMEOUT_PENALTY = 10;
    private static final int MAX_TIMEOUT_PENALTY = 30;

    private final ReportMapper reportMapper;
    private final ReportEvidenceMapper reportEvidenceMapper;
    private final TaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final CreditLogMapper creditLogMapper;
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
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该举报");
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
        if (currentUserId.equals(task.getPublisherId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能举报自己发布的任务");
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
    public ReportSubmissionVO submitUserReport(Long userId, ReportCreateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能举报自己");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Report report = new Report();
        report.setReporterId(currentUserId);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(userId);
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
                userId,
                request.getReason().trim(),
                evidenceImageIds,
                report.getCreatedAt()
        );
    }

    @Transactional
    public ReportSubmissionVO submitTimeoutOrderReport(Long orderId, ReportCreateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!currentUserId.equals(order.getPublisherId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT, "只有发布方可以举报超时服务");
        }
        if (!OrderStatus.TIMEOUT.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "只有已超时订单可以提交超时举报");
        }
        if (order.getServiceProviderId() == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单没有可举报的服务方");
        }

        Report existing = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, currentUserId)
                .eq(Report::getTargetType, ReportTargetType.USER)
                .eq(Report::getTargetId, order.getServiceProviderId())
                .eq(Report::getRelatedOrderId, orderId)
                .eq(Report::getReasonType, ReportReasonType.TIMEOUT)
                .last("LIMIT 1"));
        if (existing != null) {
            if (ReportStatus.PENDING.equals(existing.getStatus()) || ReportStatus.PROCESSING.equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED, "超时举报处理中，请勿重复提交");
            }
            throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED, "该超时举报已处理，不能重复提交");
        }

        Report report = new Report();
        report.setReporterId(currentUserId);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(order.getServiceProviderId());
        report.setRelatedOrderId(orderId);
        report.setReasonType(ReportReasonType.TIMEOUT);
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
                orderId,
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已处理举报的状态只能是已解决或已驳回");
        }

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        report.setStatus(request.getStatus());
        report.setResult(request.getResult().trim());
        report.setProcessedBy(currentUserId);
        report.setProcessedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        if (ReportStatus.RESOLVED.equals(request.getStatus()) && ReportReasonType.TIMEOUT.equals(report.getReasonType())) {
            applyTimeoutCreditPenalty(report, request);
        }

        if (report.getRelatedOrderId() != null) {
            notificationService.createReportResultOrderNotification(report.getReporterId(), report.getRelatedOrderId(), request.getResult().trim());
            notificationService.createReportResultOrderNotification(report.getTargetId(), report.getRelatedOrderId(), request.getResult().trim());
        } else {
            notificationService.createReportResultNotification(report.getReporterId(), report.getTargetId(), request.getResult().trim());
        }
    }

    private void applyTimeoutCreditPenalty(Report report, ReportProcessRequest request) {
        Integer requestedPenalty = request.getCreditPenalty();
        int penalty = requestedPenalty == null ? DEFAULT_TIMEOUT_PENALTY : requestedPenalty;
        if (penalty < 1 || penalty > MAX_TIMEOUT_PENALTY) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "超时扣分必须在 1 到 30 之间");
        }

        Long userId = report.getTargetId();
        CreditLog latest = creditLogMapper.selectOne(new LambdaQueryWrapper<CreditLog>()
                .eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getId)
                .last("LIMIT 1"));
        int scoreBefore = clampCreditScore(latest != null ? latest.getScoreAfter() : DEFAULT_CREDIT_SCORE);
        int scoreAfter = clampCreditScore(scoreBefore - penalty);

        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeAmount(scoreAfter - scoreBefore);
        creditLog.setScoreBefore(scoreBefore);
        creditLog.setScoreAfter(scoreAfter);
        creditLog.setReason("超时订单举报成立，扣减信用分");
        creditLog.setRelatedOrderId(report.getRelatedOrderId());
        creditLogMapper.insert(creditLog);
    }

    private int clampCreditScore(int score) {
        return Math.max(MIN_CREDIT_SCORE, Math.min(MAX_CREDIT_SCORE, score));
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "页码必须大于等于 1");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每页数量必须大于等于 1");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private ReportItemVO toReportItemVO(Report report) {
        return new ReportItemVO(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getRelatedOrderId(),
                report.getReasonType(),
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
                report.getRelatedOrderId(),
                report.getReasonType(),
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
