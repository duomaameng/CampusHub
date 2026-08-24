package com.campushub.dto.task;

import com.campushub.enums.RewardType;
import com.campushub.enums.RewardPaymentMethod;
import com.campushub.enums.TaskCategory;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.math.BigDecimal;
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

    private BigDecimal rewardAmount;

    private RewardPaymentMethod paymentMethod;

    @Future(message = "截止时间必须晚于当前时间")
    private LocalDateTime deadline;

    private Boolean anonymous;

    private List<Long> imageIds;

    private List<Long> fileIds;

    private Map<String, Object> categoryFields;
}
