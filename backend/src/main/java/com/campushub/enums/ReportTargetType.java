package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ReportTargetType {
    TASK("TASK"),
    ORDER_MESSAGE("ORDER_MESSAGE"),
    REVIEW("REVIEW"),
    USER("USER");

    @EnumValue
    private final String value;

    ReportTargetType(String value) {
        this.value = value;
    }
}
