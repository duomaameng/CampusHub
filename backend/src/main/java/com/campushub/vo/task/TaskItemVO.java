package com.campushub.vo.task;

import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/*
这是任务列表和任务详情最核心的返回类。
它把很多信息组合在一起：

发布者昵称
头像
分类
标题
描述
校区
状态
配图
申请数
收藏数
分类附加字段
它的意义是：前端任务大厅和任务详情页主要靠它渲染。
*/
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
