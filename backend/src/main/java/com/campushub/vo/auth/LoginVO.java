package com.campushub.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String tokenType;
    private long expiresIn;
    private LoginUser user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
        private Long id;
        private String email;
        private String role;
        private String status;
        private boolean verified;
        private String nickname;
        private String avatarUrl;
    }
}
