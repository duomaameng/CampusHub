package com.campushub.vo.order;

import com.campushub.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/*
这是订单聊天消息的返回对象。
支持：

文字消息
图片消息
发送人昵称
时间
它的意义是：订单详情页里的聊天区靠它显示。
*/
@Data
@AllArgsConstructor
public class OrderMessageVO {

    private Long id;
    private Long orderId;
    private Long senderId;
    private String senderNickname;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
}
