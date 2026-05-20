package com.campushub.service;

import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.dto.response.PublicProfileResponse;
import com.campushub.dto.response.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUser();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    PublicProfileResponse getPublicProfile(Long userId);
}
