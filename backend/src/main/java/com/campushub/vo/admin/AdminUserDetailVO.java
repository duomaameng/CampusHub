package com.campushub.vo.admin;

import com.campushub.enums.UserRole;
import com.campushub.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailVO {

    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private UserRole role;
    private UserStatus status;
    private Boolean verified;
    private Integer creditScore;
    private LocalDateTime createdAt;
}
