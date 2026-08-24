package com.campushub.service;

import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.vo.user.PublicProfileVO;
import com.campushub.vo.user.UserCreditVO;
import com.campushub.vo.user.UserProfileVO;
import com.campushub.vo.user.UserReviewItemVO;
import com.campushub.vo.message.ChatUserVO;

import java.util.List;

public interface UserService {

    UserProfileVO getCurrentUser();

    UserProfileVO updateProfile(UpdateProfileRequest request);

    PublicProfileVO getPublicProfile(Long userId);

    void deleteCurrentUser();

    List<UserReviewItemVO> getUserReviews(Long userId);

    UserCreditVO getUserCredit(Long userId);

    List<ChatUserVO> searchChatUsers(String keyword);
}
