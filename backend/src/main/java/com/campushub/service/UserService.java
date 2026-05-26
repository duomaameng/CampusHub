package com.campushub.service;

import com.campushub.common.PageResult;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.dto.response.CreditInfoResponse;
import com.campushub.dto.response.PublicProfileResponse;
import com.campushub.dto.response.UserProfileResponse;
import com.campushub.vo.order.ReviewItemVO;

public interface UserService {

    UserProfileResponse getCurrentUser();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    void deleteAccount();

    PublicProfileResponse getPublicProfile(Long userId);

    PageResult<ReviewItemVO> getUserReviews(Long userId, int page, int size);

    CreditInfoResponse getUserCredit(Long userId);
}
