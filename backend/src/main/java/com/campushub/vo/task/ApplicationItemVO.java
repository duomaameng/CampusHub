package com.campushub.vo.task;

import com.campushub.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;
/*
这是“某个任务的接单申请列表”的展示对象。
里面有：

申请人昵称
头像
信用分
留言
申请状态
时间
它的意义是：发布者查看谁申请接单时，就看这一类数据。
*/
@Data
public class ApplicationItemVO {

    private Long id;
    private Long taskId;
    private Long applicantId;
    private String applicantNickname;
    private String applicantAvatarUrl;
    private Integer applicantCreditScore;
    private String message;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
