package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum RewardPaymentMethod {
    WECHAT("WECHAT"),
    ALIPAY("ALIPAY"),
    CASH("CASH");

    @EnumValue
    private final String value;

    RewardPaymentMethod(String value) {
        this.value = value;
    }
}
