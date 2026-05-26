package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.admin.AdminAnnouncementCreateRequest;
import com.campushub.dto.admin.AdminAnnouncementUpdateRequest;
import com.campushub.dto.admin.AdminUserStatusRequest;
import com.campushub.enums.UserStatus;
import com.campushub.service.AdminService;
import com.campushub.vo.admin.AdminUserItemVO;
import com.campushub.vo.admin.AdminUserStatusVO;
import com.campushub.vo.announcement.AnnouncementItemVO;
import com.campushub.vo.announcement.AnnouncementPublishVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserItemVO>> users(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) UserStatus status,
                                                          @RequestParam(required = false) Boolean verified) {
        return ApiResponse.success(adminService.listUsers(page, size, keyword, status, verified));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<AdminUserStatusVO> updateUserStatus(@PathVariable Long userId,
                                                           @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.success(adminService.updateUserStatus(userId, request.getStatus(), request.getReason()));
    }

    @GetMapping("/announcements")
    public ApiResponse<PageResult<AnnouncementItemVO>> announcements(@RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listAnnouncements(page, size));
    }

    @PostMapping("/announcements")
    public ApiResponse<AnnouncementPublishVO> createAnnouncement(@Valid @RequestBody AdminAnnouncementCreateRequest request) {
        return ApiResponse.success(adminService.createAnnouncement(request));
    }

    @PatchMapping("/announcements/{announcementId}")
    public ApiResponse<AnnouncementPublishVO> updateAnnouncement(@PathVariable Long announcementId,
                                                                 @Valid @RequestBody AdminAnnouncementUpdateRequest request) {
        return ApiResponse.success(adminService.updateAnnouncement(announcementId, request));
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long announcementId) {
        adminService.deleteAnnouncement(announcementId);
        return ApiResponse.success();
    }
}
