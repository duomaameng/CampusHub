package com.campushub.dto.task;

import com.campushub.enums.RewardType;
import com.campushub.enums.RewardPaymentMethod;
import com.campushub.enums.TaskCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TaskCreateRequest {

    @NotNull(message = "需求分类不能为空")
    private TaskCategory category;

    @NotBlank(message = "需求标题不能为空")
    private String title;

    @NotBlank(message = "需求描述不能为空")
    private String description;

    @NotBlank(message = "校区不能为空")
    private String campus;

    @NotNull(message = "报酬类型不能为空")
    private RewardType rewardType;

    private BigDecimal rewardAmount;

    private RewardPaymentMethod paymentMethod;

    @NotNull(message = "截止时间不能为空")
    @Future(message = "截止时间必须晚于当前时间")
    private LocalDateTime deadline;

    @NotNull(message = "匿名设置不能为空")
    private Boolean anonymous;

    private List<Long> imageIds;

    private List<Long> fileIds;

    private Map<String, Object> categoryFields;
}
