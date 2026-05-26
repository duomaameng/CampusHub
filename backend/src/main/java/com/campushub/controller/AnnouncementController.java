package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.service.AdminService;
import com.campushub.vo.announcement.AnnouncementItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResult<AnnouncementItemVO>> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.listPublicAnnouncements(page, size));
    }
}
