package com.campushub.vo.admin;

import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTaskItemVO {

    private Long id;
    private String title;
    private TaskCategory category;
    private Long publisherId;
    private TaskStatus status;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
}
