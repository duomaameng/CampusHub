package com.campushub.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用错误 40000-40009
    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40001, "未登录或 Token 已过期"),
    FORBIDDEN(40002, "权限不足"),
    NOT_FOUND(40003, "资源不存在"),
    INTERNAL_ERROR(40004, "服务器内部错误"),
    VALIDATION_ERROR(40005, "参数校验失败"),

    // 认证模块 40010-40039
    EMAIL_ALREADY_EXISTS(40010, "该邮箱已注册"),
    EMAIL_NOT_FOUND(40011, "该邮箱未注册"),
    PASSWORD_INCORRECT(40012, "密码错误"),
    ACCOUNT_DISABLED(40013, "账号已被禁用"),
    ACCOUNT_LOCKED(40014, "账号已被锁定，请稍后再试"),
    EMAIL_NOT_VERIFIED(40015, "邮箱未验证"),
    VERIFICATION_CODE_INVALID(40016, "验证码无效或已过期"),
    VERIFICATION_CODE_EXPIRED(40017, "验证码已过期"),
    PASSWORD_SAME_AS_OLD(40018, "新密码不能与旧密码相同"),
    UNAUTHORIZED_OPERATION(40019, "未完成校园身份认证，无法执行此操作"),
    EMAIL_SEND_FAILED(40020, "验证码邮件发送失败"),

    // 需求模块 40100-40119
    TASK_NOT_FOUND(40100, "需求不存在"),
    TASK_NOT_OPEN(40101, "需求当前状态不可操作"),
    TASK_NOT_OWNER(40102, "只能操作自己发布的需求"),
    TASK_HAS_APPLICATION(40103, "需求已有接单申请，不可编辑或删除"),
    TASK_ALREADY_FAVORITED(40104, "已收藏该需求"),
    TASK_NOT_FAVORITED(40105, "未收藏该需求"),

    // 订单模块 40200-40219
    ORDER_NOT_FOUND(40200, "订单不存在"),
    ORDER_STATUS_INVALID(40201, "当前订单状态不允许此操作"),
    ORDER_NOT_PARTICIPANT(40202, "非订单参与方无权操作"),
    ORDER_ALREADY_CANCELLED(40203, "订单已取消"),
    ORDER_ALREADY_COMPLETED(40204, "订单已完成"),
    APPLICATION_NOT_FOUND(40205, "接单申请不存在"),
    APPLICATION_ALREADY_PROCESSED(40206, "接单申请已被处理"),
    ALREADY_APPLIED(40207, "已提交过接单申请"),
    CANNOT_APPLY_OWN_TASK(40208, "不能申请自己发布的需求"),
    TASK_ALREADY_TAKEN(40209, "该需求已被其他人接走"),

    // 评价模块 40400-40409
    REVIEW_NOT_FOUND(40400, "评价不存在"),
    REVIEW_ALREADY_EXISTS(40401, "已提交过评价"),
    REVIEW_ORDER_NOT_COMPLETED(40402, "订单未完成，无法评价"),
    REVIEW_PERIOD_EXPIRED(40403, "评价期限已过"),

    // 文件模块 40500-40509
    FILE_TOO_LARGE(40500, "文件大小超过限制"),
    FILE_TYPE_NOT_ALLOWED(40501, "文件类型不允许"),
    FILE_UPLOAD_FAILED(40502, "文件上传失败"),

    // 通知模块 40600-40609
    NOTIFICATION_NOT_FOUND(40600, "通知不存在"),

    // 举报模块 40700-40709
    REPORT_NOT_FOUND(40700, "举报不存在"),
    REPORT_ALREADY_HANDLED(40701, "举报已被处理"),

    // 消息模块 40800-40809
    MESSAGE_EMPTY(40800, "消息内容不能为空"),
    MESSAGE_NOT_PARTICIPANT(40801, "非订单参与方无法发送消息"),

    // 后台管理 40900-40909
    ADMIN_REQUIRED(40900, "需要管理员权限"),
    USER_NOT_FOUND(40901, "用户不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
