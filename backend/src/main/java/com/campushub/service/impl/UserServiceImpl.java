package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.dto.response.CreditInfoResponse;
import com.campushub.dto.response.PublicProfileResponse;
import com.campushub.dto.response.UserProfileResponse;
import com.campushub.entity.FileRecord;
import com.campushub.entity.Review;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.UploadBusinessType;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.service.FileService;
import com.campushub.service.UserService;
import com.campushub.vo.order.ReviewItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final ReviewMapper reviewMapper;
    private final FileService fileService;

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
            if (request.getAvatarUrl().isBlank()) {
                profile.setAvatarUrl(null);
            } else {
                FileRecord avatar = fileService.requireOwnedFile(request.getAvatarUrl(), UploadBusinessType.AVATAR);
                profile.setAvatarUrl(avatar.getFileUrl());
            }
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

    @Override
    @Transactional
    public void deleteAccount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(UserStatus.ANONYMIZED);
        user.setEmail("deleted_" + userId + "@anonymized.local");
        userMapper.updateById(user);
    }

    @Override
    public PageResult<ReviewItemVO> getUserReviews(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        IPage<Review> reviewPage = reviewMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreatedAt));

        List<ReviewItemVO> records = reviewPage.getRecords().stream()
                .map(r -> {
                    UserProfile reviewerProfile = userProfileMapper.selectOne(
                            new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, r.getReviewerId()));
                    return new ReviewItemVO(
                            r.getId(), r.getOrderId(), r.getReviewerId(),
                            reviewerProfile != null ? reviewerProfile.getNickname() : "",
                            r.getRevieweeId(), "", r.getRating(), r.getContent(), r.getCreatedAt());
                })
                .collect(Collectors.toList());

        return PageResult.of(reviewPage.getTotal(), page, size, records);
    }

    @Override
    public CreditInfoResponse getUserCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        long completedOrders = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getRevieweeId, userId));
        double avgRating = 0;
        if (completedOrders > 0) {
            List<Review> reviews = reviewMapper.selectList(
                    new LambdaQueryWrapper<Review>()
                            .eq(Review::getRevieweeId, userId)
                            .orderByDesc(Review::getCreatedAt)
                            .last("LIMIT 10"));
            avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        }

        List<ReviewItemVO> recentReviews = reviewMapper.selectList(
                        new LambdaQueryWrapper<Review>()
                                .eq(Review::getRevieweeId, userId)
                                .orderByDesc(Review::getCreatedAt)
                                .last("LIMIT 5"))
                .stream()
                .map(r -> {
                    UserProfile reviewerProfile = userProfileMapper.selectOne(
                            new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, r.getReviewerId()));
                    return new ReviewItemVO(
                            r.getId(), r.getOrderId(), r.getReviewerId(),
                            reviewerProfile != null ? reviewerProfile.getNickname() : "",
                            r.getRevieweeId(), "", r.getRating(), r.getContent(), r.getCreatedAt());
                })
                .collect(Collectors.toList());

        return CreditInfoResponse.builder()
                .userId(userId)
                .score(100)
                .completedOrders((int) completedOrders)
                .praiseRate(Math.round(avgRating * 100.0) / 100.0)
                .recentReviews(recentReviews)
                .build();
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
