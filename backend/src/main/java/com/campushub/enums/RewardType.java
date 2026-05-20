package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum RewardType {
    CASH("CASH"),
    NEGOTIABLE("NEGOTIABLE"),
    CREDIT_INTENT("CREDIT_INTENT");

    @EnumValue
    private final String value;

    RewardType(String value) {
        this.value = value;
    }
}
