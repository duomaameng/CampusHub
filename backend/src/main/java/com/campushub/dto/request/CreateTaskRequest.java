package com.campushub.dto.request;

import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateTaskRequest {

    @NotNull(message = "需求类别不能为空")
    private TaskCategory category;

    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度 2-100 个字符")
    private String title;

    @NotBlank(message = "描述不能为空")
    @Size(min = 10, max = 2000, message = "描述长度 10-2000 个字符")
    private String description;

    @NotBlank(message = "校区不能为空")
    private String campus;

    private String locationDetail;

    @NotNull(message = "报酬类型不能为空")
    private RewardType rewardType;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;

    private Boolean anonymous = false;

    @Size(max = 9, message = "最多上传 9 张图片")
    private List<Long> imageIds;

    private String categoryFields;
}
