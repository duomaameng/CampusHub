package com.campushub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteOrderRequest {

    private Long proofImageId;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String note;
}
