package com.campushub.dto.report;

import com.campushub.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportProcessRequest {

    @NotNull(message = "举报处理状态不能为空")
    private ReportStatus status;

    @NotBlank(message = "处理结果不能为空")
    private String result;

    private Integer creditPenalty;
}
