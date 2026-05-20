package com.campushub.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/*
这个类负责接评价数据：

几星
评价内容
它的意义是：订单完成后，提交评价时用它接参数。
*/
@Data
public class ReviewCreateRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String content;
}
