package com.campushub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String role;
    private String status;
    private boolean verified;
    private ProfileDetail profile;
    private CreditSummary credit;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileDetail {
        private String nickname;
        private String avatarUrl;
        private String gender;
        private String grade;
        private String college;
        private String bio;
        private String campus;
        private String contact;
        private Boolean contactVisible;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditSummary {
        private Integer score;
        private Integer completedOrders;
        private Double praiseRate;
    }
}
