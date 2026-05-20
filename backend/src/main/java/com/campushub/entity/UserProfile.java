package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private String gender;

    private String grade;

    private String college;

    private String bio;

    private String campus;

    private String contact;

    private Boolean contactVisible;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
