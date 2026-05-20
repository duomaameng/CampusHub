package com.campushub.dto.response;

import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListItem {
    private Long id;
    private Long publisherId;
    private String publisherNickname;
    private TaskCategory category;
    private String title;
    private String description;
    private String campus;
    private RewardType rewardType;
    private LocalDateTime deadline;
    private TaskStatus status;
    private Boolean anonymous;
    private List<String> imageUrls;
    private Integer applicationCount;
    private Integer favoriteCount;
    private Boolean isFavorited;
    private LocalDateTime createdAt;
}
