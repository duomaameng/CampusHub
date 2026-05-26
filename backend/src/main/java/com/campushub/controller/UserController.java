package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.dto.response.CreditInfoResponse;
import com.campushub.dto.response.PublicProfileResponse;
import com.campushub.dto.response.UserProfileResponse;
import com.campushub.service.UserService;
import com.campushub.vo.order.ReviewItemVO;
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

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount() {
        userService.deleteAccount();
        return ApiResponse.success();
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<PublicProfileResponse> getPublicProfile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getPublicProfile(userId));
    }

    @GetMapping("/{userId}/reviews")
    public ApiResponse<PageResult<ReviewItemVO>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.getUserReviews(userId, page, size));
    }

    @GetMapping("/{userId}/credit")
    public ApiResponse<CreditInfoResponse> getUserCredit(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserCredit(userId));
    }
}
