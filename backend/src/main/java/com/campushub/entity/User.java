package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.enums.UserRole;
import com.campushub.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String passwordHash;

    private UserRole role;

    private UserStatus status;

    private Boolean verified;

    private String studentNoMasked;

    private Integer loginFailures;

    private LocalDateTime lockedUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
