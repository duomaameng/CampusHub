package com.campushub.dto.admin;

import com.campushub.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserStatusRequest {

    @NotNull(message = "用户状态不能为空")
    private UserStatus status;

    @Size(max = 500, message = "处理原因不能超过 500 个字符")
    private String reason;
}
