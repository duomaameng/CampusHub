package com.campushub.vo.order;

import com.campushub.enums.RewardType;
import lombok.Data;

import java.util.List;
/*
这是订单详情页最核心的返回类。
它比 OrderItemVO 更完整，多了：

任务描述
校区
报酬类型
完成凭证
状态日志
聊天消息
它的意义是：前端订单详情页打开后，主要就靠它。
*/
@Data
public class OrderDetailVO extends OrderItemVO {

    private String taskDescription;
    private String campus;
    private RewardType rewardType;
    private String proofImageUrl;
    private String completionNote;
    private List<OrderStatusLogVO> statusLogs;
    private List<OrderMessageVO> messages;
}
