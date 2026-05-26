package com.campushub.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReportCreateRequest {

    @NotBlank
    private String reason;

    private List<Long> evidenceImageIds;
}
