package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum NotificationType {
    APPLICATION("APPLICATION"),
    ORDER_STATUS("ORDER_STATUS"),
    ORDER_MESSAGE("ORDER_MESSAGE"),
    REVIEW_REQUEST("REVIEW_REQUEST"),
    REPORT_RESULT("REPORT_RESULT");

    @EnumValue
    private final String value;

    NotificationType(String value) {
        this.value = value;
    }
}
