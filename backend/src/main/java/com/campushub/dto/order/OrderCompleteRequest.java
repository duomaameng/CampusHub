package com.campushub.dto.order;

import lombok.Data;
/*
这个类负责接“提交完成”时带来的参数，比如：

完成凭证图片 ID
备注说明
它的意义是：服务方提交完成订单时，把完成证据和说明交给后端。
*/
@Data
public class OrderCompleteRequest {

    private Long proofImageId;

    private String note;
}
