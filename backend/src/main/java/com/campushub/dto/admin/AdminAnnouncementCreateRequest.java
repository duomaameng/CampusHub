package com.campushub.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminAnnouncementCreateRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过 100 个字符")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 5000, message = "公告内容不能超过 5000 个字符")
    private String content;

    @Size(max = 20, message = "公告优先级不能超过 20 个字符")
    private String priority;
}
