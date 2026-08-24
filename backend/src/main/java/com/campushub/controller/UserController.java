package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.service.UserService;
import com.campushub.vo.user.PublicProfileVO;
import com.campushub.vo.user.UserCreditVO;
import com.campushub.vo.user.UserProfileVO;
import com.campushub.vo.user.UserReviewItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileVO> getCurrentUser() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ApiResponse.success();
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<PublicProfileVO> getPublicProfile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getPublicProfile(userId));
    }

    @GetMapping("/{userId}/reviews")
    public ApiResponse<List<UserReviewItemVO>> getUserReviews(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserReviews(userId));
    }

    @GetMapping("/{userId}/credit")
    public ApiResponse<UserCreditVO> getUserCredit(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserCredit(userId));
    }
}
