package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.dto.response.PublicProfileResponse;
import com.campushub.dto.response.UserProfileResponse;
import com.campushub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getCurrentUser() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<PublicProfileResponse> getPublicProfile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getPublicProfile(userId));
    }
}
