package com.campushub.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/*
这个类负责接“取消订单”的原因。

它的意义是：让取消订单不是无理由发生，而是带着解释。
*/
@Data
public class OrderCancelRequest {

    @NotBlank
    private String reason;
}
