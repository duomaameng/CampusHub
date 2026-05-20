package com.campushub.dto.report;

import com.campushub.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/*
这个类负责接管理员处理举报时的参数：

处理后的状态
处理结果说明
它的意义是：后台管理员审核举报时，用它接处理结果。
*/
@Data
public class ReportProcessRequest {

    @NotNull
    private ReportStatus status;

    @NotBlank
    private String result;
}
