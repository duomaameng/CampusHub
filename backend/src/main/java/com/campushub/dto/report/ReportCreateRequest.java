package com.campushub.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReportCreateRequest {

    @NotBlank(message = "举报原因不能为空")
    private String reason;

    private List<Long> evidenceImageIds;
}
