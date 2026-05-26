package com.campushub.dto.task;

import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TaskCreateRequest {

    @NotNull
    private TaskCategory category;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String campus;

    @NotNull
    private RewardType rewardType;

    @NotNull
    @Future
    private LocalDateTime deadline;

    @NotNull
    private Boolean anonymous;

    private List<Long> imageIds;

    private Map<String, Object> categoryFields;
}
