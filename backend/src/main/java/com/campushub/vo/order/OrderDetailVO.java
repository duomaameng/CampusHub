package com.campushub.vo.order;

import com.campushub.enums.RewardType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
}
