package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ReportReasonType {
    FRAUD("FRAUD"),
    ABUSE("ABUSE"),
    SPAM("SPAM"),
    ILLEGAL("ILLEGAL"),
    OTHER("OTHER");

    @EnumValue
    private final String value;

    ReportReasonType(String value) {
        this.value = value;
    }
}
