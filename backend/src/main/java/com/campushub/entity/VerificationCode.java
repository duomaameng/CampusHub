package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_code")
public class VerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String code;

    private String purpose;

    private LocalDateTime expiresAt;

    private Boolean used;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
