package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.dto.response.PublicProfileResponse;
import com.campushub.dto.response.UserProfileResponse;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse getCurrentUser() {
        Long userId = SecurityUtils.requireCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(request.getNickname())) {
            profile.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getGrade() != null) {
            profile.setGrade(request.getGrade());
        }
        if (request.getCollege() != null) {
            profile.setCollege(request.getCollege());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getCampus() != null) {
            profile.setCampus(request.getCampus());
        }
        if (request.getContact() != null) {
            profile.setContact(request.getContact());
        }
        if (request.getContactVisible() != null) {
            profile.setContactVisible(request.getContactVisible());
        }
        userProfileMapper.updateById(profile);

        User user = userMapper.selectById(userId);
        return buildProfileResponse(user);
    }

    @Override
    public PublicProfileResponse getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        PublicProfileResponse.PublicProfileResponseBuilder builder = PublicProfileResponse.builder()
                .userId(user.getId())
                .nickname(profile != null ? profile.getNickname() : "")
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .gender(profile != null ? profile.getGender() : null)
                .college(profile != null ? profile.getCollege() : null)
                .campus(profile != null ? profile.getCampus() : null)
                .verified(user.getVerified())
                .creditScore(100)
                .completedOrders(0)
                .praiseRate(1.0)
                .memberSince(user.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE));

        if (profile != null && profile.getContactVisible()) {
            builder.contact(profile.getContact());
            builder.contactVisible(true);
        } else {
            builder.contact(null);
            builder.contactVisible(false);
        }

        return builder.build();
    }

    private UserProfileResponse buildProfileResponse(User user) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().getValue())
                .status(user.getStatus().getValue())
                .verified(user.getVerified())
                .profile(UserProfileResponse.ProfileDetail.builder()
                        .nickname(profile != null ? profile.getNickname() : "")
                        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                        .gender(profile != null ? profile.getGender() : null)
                        .grade(profile != null ? profile.getGrade() : null)
                        .college(profile != null ? profile.getCollege() : null)
                        .bio(profile != null ? profile.getBio() : null)
                        .campus(profile != null ? profile.getCampus() : null)
                        .contact(profile != null ? profile.getContact() : null)
                        .contactVisible(profile != null && profile.getContactVisible())
                        .build())
                .credit(UserProfileResponse.CreditSummary.builder()
                        .score(100)
                        .completedOrders(0)
                        .praiseRate(1.0)
                        .build())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
