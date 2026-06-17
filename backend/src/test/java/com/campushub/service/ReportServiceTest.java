package com.campushub.service;

import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.dto.report.ReportProcessRequest;
import com.campushub.entity.CreditLog;
import com.campushub.entity.FileRecord;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportMapper reportMapper;
    @Mock private ReportEvidenceMapper reportEvidenceMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private UserMapper userMapper;
    @Mock private CreditLogMapper creditLogMapper;
    @Mock private NotificationService notificationService;
    @Mock private FileService fileService;

    @InjectMocks
    private ReportService reportService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::requireCurrentUserId).thenReturn(10001L);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void shouldPersistReportEvidenceWithReportEvidencePurpose() {
        Task task = new Task();
        task.setId(3001L);
        when(taskMapper.selectById(3001L)).thenReturn(task);
        when(reportMapper.insert(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(9001L);
            return 1;
        });
        FileRecord evidenceFile = new FileRecord();
        evidenceFile.setId(15001L);
        when(fileService.requireOwnedFile(eq(15001L), eq(UploadBusinessType.REPORT_EVIDENCE))).thenReturn(evidenceFile);

        ReportCreateRequest request = new ReportCreateRequest();
        request.setReason("fraud evidence");
        request.setEvidenceImageIds(List.of(15001L));

        reportService.submitTaskReport(3001L, request);

        ArgumentCaptor<ReportEvidence> captor = ArgumentCaptor.forClass(ReportEvidence.class);
        verify(reportEvidenceMapper).insert(captor.capture());
        assertThat(captor.getValue().getReportId()).isEqualTo(9001L);
        assertThat(captor.getValue().getFileRecordId()).isEqualTo(15001L);
    }

    @Test
    void shouldSubmitTimeoutOrderReportAgainstServiceProvider() {
        Order order = new Order();
        order.setId(7001L);
        order.setPublisherId(10001L);
        order.setServiceProviderId(20002L);
        order.setStatus(OrderStatus.TIMEOUT);
        when(orderMapper.selectById(7001L)).thenReturn(order);
        when(reportMapper.selectOne(any())).thenReturn(null);
        when(reportMapper.insert(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(9101L);
            return 1;
        });
        FileRecord evidenceFile = new FileRecord();
        evidenceFile.setId(16001L);
        when(fileService.requireOwnedFile(eq(16001L), eq(UploadBusinessType.REPORT_EVIDENCE))).thenReturn(evidenceFile);

        ReportCreateRequest request = new ReportCreateRequest();
        request.setReason("服务方超时未完成");
        request.setEvidenceImageIds(List.of(16001L));

        reportService.submitTimeoutOrderReport(7001L, request);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insert(reportCaptor.capture());
        Report savedReport = reportCaptor.getValue();
        assertThat(savedReport.getReporterId()).isEqualTo(10001L);
        assertThat(savedReport.getTargetType()).isEqualTo(ReportTargetType.USER);
        assertThat(savedReport.getTargetId()).isEqualTo(20002L);
        assertThat(savedReport.getRelatedOrderId()).isEqualTo(7001L);
        assertThat(savedReport.getReasonType()).isEqualTo(ReportReasonType.TIMEOUT);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.PENDING);

        ArgumentCaptor<ReportEvidence> evidenceCaptor = ArgumentCaptor.forClass(ReportEvidence.class);
        verify(reportEvidenceMapper).insert(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue().getReportId()).isEqualTo(9101L);
        assertThat(evidenceCaptor.getValue().getFileRecordId()).isEqualTo(16001L);
    }

    @Test
    void shouldDeductCreditWhenTimeoutReportResolved() {
        Report report = new Report();
        report.setId(9102L);
        report.setReporterId(10001L);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(20002L);
        report.setRelatedOrderId(7002L);
        report.setReasonType(ReportReasonType.TIMEOUT);
        report.setStatus(ReportStatus.PENDING);
        when(reportMapper.selectById(9102L)).thenReturn(report);

        CreditLog latestCredit = new CreditLog();
        latestCredit.setScoreAfter(86);
        when(creditLogMapper.selectOne(any())).thenReturn(latestCredit);

        ReportProcessRequest request = new ReportProcessRequest();
        request.setStatus(ReportStatus.RESOLVED);
        request.setResult("举报成立");
        request.setCreditPenalty(12);

        reportService.processReport(9102L, request);

        ArgumentCaptor<CreditLog> creditCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(creditCaptor.capture());
        CreditLog savedCreditLog = creditCaptor.getValue();
        assertThat(savedCreditLog.getUserId()).isEqualTo(20002L);
        assertThat(savedCreditLog.getScoreBefore()).isEqualTo(86);
        assertThat(savedCreditLog.getScoreAfter()).isEqualTo(74);
        assertThat(savedCreditLog.getChangeAmount()).isEqualTo(-12);
        assertThat(savedCreditLog.getRelatedOrderId()).isEqualTo(7002L);
        verify(notificationService).createReportResultOrderNotification(10001L, 7002L, "举报成立");
        verify(notificationService).createReportResultOrderNotification(20002L, 7002L, "举报成立");
    }
}
