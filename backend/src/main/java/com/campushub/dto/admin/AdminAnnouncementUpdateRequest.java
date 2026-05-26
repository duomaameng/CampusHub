package com.campushub.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminAnnouncementUpdateRequest {

    @Size(min = 1, max = 100, message = "Announcement title must be within 100 characters")
    private String title;

    @Size(min = 1, max = 5000, message = "Announcement content must be within 5000 characters")
    private String content;

    @Size(max = 20, message = "Announcement priority must be within 20 characters")
    private String priority;

    private Boolean isActive;
}
