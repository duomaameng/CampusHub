package com.campushub.dto.response;

import com.campushub.vo.order.ReviewItemVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditInfoResponse {
    private Long userId;
    private int score;
    private int completedOrders;
    private double praiseRate;
    private List<ReviewItemVO> recentReviews;
}
