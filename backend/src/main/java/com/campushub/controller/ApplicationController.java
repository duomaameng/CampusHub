package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.ApplyTaskRequest;
import com.campushub.dto.response.ApplicationResponse;
import com.campushub.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/api/tasks/{taskId}/applications")
    public ApiResponse<List<ApplicationResponse>> listApplications(@PathVariable Long taskId) {
        return ApiResponse.success(applicationService.listApplications(taskId));
    }

    @PostMapping("/api/tasks/{taskId}/applications")
    public ApiResponse<ApplicationResponse> apply(@PathVariable Long taskId,
                                                    @Valid @RequestBody ApplyTaskRequest request) {
        return ApiResponse.success(applicationService.apply(taskId, request.getMessage()));
    }

    @PostMapping("/api/applications/{applicationId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long applicationId) {
        applicationService.confirm(applicationId);
        return ApiResponse.success();
    }

    @PostMapping("/api/applications/{applicationId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long applicationId) {
        applicationService.reject(applicationId);
        return ApiResponse.success();
    }
}
