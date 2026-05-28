package com.campushub.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReviewItemVO {
    private Long reviewId;
    private Long orderId;
    private Long reviewerId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
