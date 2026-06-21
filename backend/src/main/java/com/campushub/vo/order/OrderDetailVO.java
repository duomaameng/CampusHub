package com.campushub.vo.order;

import com.campushub.enums.RewardType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import com.campushub.vo.task.TaskFileVO;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderItemVO {

    private String taskDescription;
    private String campus;
    private RewardType rewardType;
    private String proofImageUrl;
    private String completionNote;
    private List<OrderStatusLogVO> statusLogs;
    private List<OrderMessageVO> messages;
    private List<TaskFileVO> taskFiles;
    private boolean taskFileDownloadAllowed;
    private List<String> taskImageUrls;
}
