package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum MessageType {
    TEXT("TEXT"),
    IMAGE("IMAGE");

    @EnumValue
    private final String value;

    MessageType(String value) {
        this.value = value;
    }
}
