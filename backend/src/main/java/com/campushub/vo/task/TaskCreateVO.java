package com.campushub.vo.task;

import com.campushub.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
发布任务成功后返回：

新任务 ID
当前状态
创建时间
它的意义是：告诉前端“任务已经建好了，编号是多少”。
*/
@Data
@AllArgsConstructor
public class TaskCreateVO {

    private Long id;
    private TaskStatus status;
    private LocalDateTime createdAt;
}
