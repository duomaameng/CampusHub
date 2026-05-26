package com.campushub.dto.order;

import lombok.Data;

@Data
public class OrderCompleteRequest {

    private Long proofImageId;

    private String note;
}
