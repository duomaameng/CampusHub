package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING_CONFIRM("PENDING_CONFIRM"),
    IN_PROGRESS("IN_PROGRESS"),
    PENDING_COMPLETION("PENDING_COMPLETION"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    TIMEOUT("TIMEOUT"),
    DISPUTE("DISPUTE"),
    REVIEWED("REVIEWED");

    @EnumValue
    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }
}
