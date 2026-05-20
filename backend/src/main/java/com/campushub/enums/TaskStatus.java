package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskStatus {
    OPEN("OPEN"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED");

    @EnumValue
    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }
}
