package com.campushub.vo.order;

import com.campushub.enums.RewardType;
import com.campushub.enums.RewardPaymentMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import com.campushub.vo.task.TaskFileVO;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderItemVO {

    private String taskDescription;
    private String campus;
    private RewardType rewardType;
    private BigDecimal rewardAmount;
    private RewardPaymentMethod paymentMethod;
    private String proofImageUrl;
    private String completionNote;
    private List<OrderStatusLogVO> statusLogs;
    private List<TaskFileVO> taskFiles;
    private boolean taskFileDownloadAllowed;
    private List<String> taskImageUrls;
}
