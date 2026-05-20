package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.service.NotificationService;
import com.campushub.vo.NotificationItemVO;
import com.campushub.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResult<NotificationItemVO>> list(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) Boolean read) {
        return ApiResponse.success(notificationService.listCurrentUserNotifications(page, size, read));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount() {
        return ApiResponse.success(new UnreadCountVO(notificationService.countUnread()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
        return ApiResponse.success();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.success();
    }
}
