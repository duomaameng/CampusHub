package com.campushub.dto.task;

import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TaskUpdateRequest {

    private TaskCategory category;

    private String title;

    private String description;

    private String campus;

    private RewardType rewardType;

    @Future
    private LocalDateTime deadline;

    private Boolean anonymous;

    private List<Long> imageIds;

    private Map<String, Object> categoryFields;
}
