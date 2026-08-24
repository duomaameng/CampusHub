package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    RESOLVED("RESOLVED"),
    REJECTED("REJECTED");

    @EnumValue
    private final String value;

    ReportStatus(String value) {
        this.value = value;
    }
}
