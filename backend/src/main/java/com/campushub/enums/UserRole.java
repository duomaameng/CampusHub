package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {
    STUDENT("STUDENT"),
    ADMIN("ADMIN");

    @EnumValue
    private final String value;

    UserRole(String value) {
        this.value = value;
    }
}
