package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.entity.FileRecord;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.FileRecordMapper;
import com.campushub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private FileRecordMapper fileRecordMapper;

    @InjectMocks
    private FileService fileService;

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
    void shouldRejectFileWhenBusinessPurposeDoesNotMatch() {
        FileRecord record = new FileRecord();
        record.setId(10L);
        record.setUserId(10001L);
        record.setPurpose(UploadBusinessType.TASK_IMAGE.name());
        when(fileRecordMapper.selectById(10L)).thenReturn(record);

        assertThatThrownBy(() -> fileService.requireOwnedFile(10L, UploadBusinessType.REPORT_EVIDENCE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldReturnOwnedFileWhenPurposeMatches() {
        FileRecord record = new FileRecord();
        record.setId(10L);
        record.setUserId(10001L);
        record.setPurpose(UploadBusinessType.REPORT_EVIDENCE.name());
        when(fileRecordMapper.selectById(10L)).thenReturn(record);

        FileRecord result = fileService.requireOwnedFile(10L, UploadBusinessType.REPORT_EVIDENCE);

        assertThat(result.getId()).isEqualTo(10L);
    }
}
