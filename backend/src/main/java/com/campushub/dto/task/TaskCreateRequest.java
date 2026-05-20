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
/*
这个类负责接“发布需求”时前端传来的数据，比如：

分类
标题
描述
校区
报酬类型
截止时间
是否匿名
图片 ID
分类附加字段
它的意义是：前端提交新任务时，后端先用它把参数接住。
*/
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
