package com.campushub.enums;
/*
这个类是一个枚举，作用是：

规定“这次上传的文件是干什么用的”。

现在里面有 4 种类型：

AVATAR
TASK_IMAGE
CHAT_IMAGE
REPORT_EVIDENCE
它的意义是：

前端上传文件时，要告诉后端这张图是头像、任务配图、聊天图片，还是举报证据
后端就能按统一规则保存，同时把用途记录进数据库
你可以把它理解成：

上传文件的“用途标签”
*/
public enum UploadBusinessType {
    AVATAR,
    TASK_IMAGE,
    CHAT_IMAGE,
    REPORT_EVIDENCE
}
