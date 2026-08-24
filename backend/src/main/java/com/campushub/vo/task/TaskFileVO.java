package com.campushub.vo.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskFileVO {
    private Long id;
    private String fileName;
    private Long fileSize;
}
