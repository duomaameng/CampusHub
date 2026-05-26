package com.campushub.vo.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

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
