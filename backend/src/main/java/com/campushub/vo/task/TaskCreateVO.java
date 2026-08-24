package com.campushub.vo.task;

import com.campushub.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TaskCreateVO {

    private Long id;
    private TaskStatus status;
    private LocalDateTime createdAt;
}
