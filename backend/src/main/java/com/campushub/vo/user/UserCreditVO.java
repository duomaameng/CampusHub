package com.campushub.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreditVO {
    private Long userId;
    private Integer score;
    private Integer completedOrders;
    private Double praiseRate;
    private List<CreditChangeItem> recentChanges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditChangeItem {
        private Integer changeAmount;
        private Integer scoreBefore;
        private Integer scoreAfter;
        private String reason;
        private Long relatedOrderId;
        private LocalDateTime createdAt;
    }
}
