package com.campushub.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
/*
这个类负责接举报提交的数据：

举报原因
举报证据图片 ID 列表
它的意义是：用户举报任务时，把举报内容传进来。
*/
@Data
public class ReportCreateRequest {

    @NotBlank
    private String reason;

    private List<Long> evidenceImageIds;
}
