package com.campushub.vo.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
这是评价记录的展示对象。
里面有：

谁评谁
评分
文本内容
时间
它的意义是：订单评价记录列表靠它显示。
*/
@Data
@AllArgsConstructor
public class ReviewItemVO {

    private Long id;
    private Long orderId;
    private Long reviewerId;
    private String reviewerNickname;
    private Long revieweeId;
    private String revieweeNickname;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
