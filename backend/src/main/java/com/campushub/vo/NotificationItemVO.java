package com.campushub.vo;

import com.campushub.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemVO {

    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private boolean read;
    private LocalDateTime createdAt;
}
