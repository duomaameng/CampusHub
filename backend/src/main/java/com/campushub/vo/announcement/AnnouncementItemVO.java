package com.campushub.vo.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AnnouncementItemVO {

    private Long id;

    private String title;

    private String content;

    private String priority;

    private Boolean isActive;

    private Long publisherId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
