package com.campushub.vo.task;

import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TaskItemVO {

    private Long id;
    private Long publisherId;
    private String publisherNickname;
    private String publisherAvatarUrl;
    private TaskCategory category;
    private String title;
    private String description;
    private String campus;
    private RewardType rewardType;
    private LocalDateTime deadline;
    private TaskStatus status;
    private Boolean anonymous;
    private List<String> imageUrls;
    private long applicationCount;
    private long favoriteCount;
    private boolean isFavorited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> categoryFields;
}
