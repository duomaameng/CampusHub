package com.campushub.dto.admin;

import com.campushub.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserStatusRequest {

    @NotNull
    private UserStatus status;

    @Size(max = 500)
    private String reason;
}
