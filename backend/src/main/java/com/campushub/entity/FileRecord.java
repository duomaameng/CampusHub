package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String purpose;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
