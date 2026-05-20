package com.campushub.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/*
这个类负责接“申请接单”时的留言内容。
也就是前端点申请接单时输入的那段话。

它的意义是：把“我为什么能接这个单”这段申请说明接进后端。
*/
@Data
public class TaskApplyRequest {

    @NotBlank
    private String message;
}
