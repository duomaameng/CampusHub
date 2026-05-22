package com.campushub.vo.admin;

import com.campushub.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatusVO {

    private Long userId;
    private UserStatus status;
}
