package com.campushub.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminAnnouncementUpdateRequest {

    @Size(min = 1, max = 100, message = "公告标题必须在 1 到 100 个字符之间")
    private String title;

    @Size(min = 1, max = 5000, message = "公告内容必须在 1 到 5000 个字符之间")
    private String content;

    @Size(max = 20, message = "公告优先级不能超过 20 个字符")
    private String priority;

    private Boolean isActive;
}
