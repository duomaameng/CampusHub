package com.campushub.vo.task;

import com.campushub.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
提交接单申请成功后返回：

申请 ID
任务 ID
申请状态
创建时间
它的意义是：告诉前端“你的申请已经提交成功”。
*/
@Data
@AllArgsConstructor
public class TaskApplyVO {

    private Long applicationId;
    private Long taskId;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
