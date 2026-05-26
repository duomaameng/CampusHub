package com.campushub.dto.report;

import com.campushub.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportProcessRequest {

    @NotNull
    private ReportStatus status;

    @NotBlank
    private String result;
}
