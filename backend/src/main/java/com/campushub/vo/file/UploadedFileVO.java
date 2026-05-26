package com.campushub.vo.file;

import com.campushub.enums.UploadBusinessType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UploadedFileVO {

    private Long id;
    private UploadBusinessType businessType;
    private String fileName;
    private String contentType;
    private Long size;
    private String url;
    private LocalDateTime createdAt;
}
