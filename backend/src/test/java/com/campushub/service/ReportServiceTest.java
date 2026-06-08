package com.campushub.service;

import com.campushub.dto.report.ReportCreateRequest;
import com.campushub.entity.FileRecord;
import com.campushub.entity.Report;
import com.campushub.entity.ReportEvidence;
import com.campushub.entity.Task;
import com.campushub.enums.UploadBusinessType;
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
    @Mock private UserMapper userMapper;
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
}
