package com.campushub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private String college;
    private String campus;
    private boolean verified;
    private String contact;
    private Boolean contactVisible;
    private Integer creditScore;
    private Integer completedOrders;
    private Double praiseRate;
    private String memberSince;
}
