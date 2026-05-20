package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED"),
    ANONYMIZED("ANONYMIZED");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}
