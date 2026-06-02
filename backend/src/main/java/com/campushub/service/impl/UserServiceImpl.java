package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.request.UpdateProfileRequest;
import com.campushub.entity.CreditLog;
import com.campushub.entity.FileRecord;
import com.campushub.entity.Order;
import com.campushub.entity.Review;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.UploadBusinessType;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.service.FileService;
import com.campushub.service.UserService;
import com.campushub.vo.user.PublicProfileVO;
import com.campushub.vo.user.UserCreditVO;
import com.campushub.vo.user.UserProfileVO;
import com.campushub.vo.user.UserReviewItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final int DEFAULT_CREDIT_SCORE = 100;
    private static final int MIN_CREDIT_SCORE = 0;
    private static final int MAX_CREDIT_SCORE = 100;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final FileService fileService;
    private final ReviewMapper reviewMapper;
    private final CreditLogMapper creditLogMapper;
    private final OrderMapper orderMapper;

    @Override
    public UserProfileVO getCurrentUser() {
        Long userId = SecurityUtils.requireCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(UpdateProfileRequest request) {
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
    public PublicProfileVO getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        UserProfileVO.CreditSummary creditSummary = buildCreditSummary(userId);

        PublicProfileVO.PublicProfileVOBuilder builder = PublicProfileVO.builder()
                .userId(user.getId())
                .nickname(profile != null ? profile.getNickname() : "")
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .gender(profile != null ? profile.getGender() : null)
                .college(profile != null ? profile.getCollege() : null)
                .campus(profile != null ? profile.getCampus() : null)
                .verified(user.getVerified())
                .creditScore(creditSummary.getScore())
                .completedOrders(creditSummary.getCompletedOrders())
                .praiseRate(creditSummary.getPraiseRate())
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
    public void deleteCurrentUser() {
        Long userId = SecurityUtils.requireCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setEmail("anonymized+" + userId + "@deleted.local");
        user.setPasswordHash("{deleted}");
        user.setStatus(UserStatus.ANONYMIZED);
        user.setVerified(false);
        user.setStudentNoMasked(null);
        user.setLoginFailures(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile != null) {
            profile.setNickname("已注销用户");
            profile.setAvatarUrl(null);
            profile.setGender(null);
            profile.setGrade(null);
            profile.setCollege(null);
            profile.setBio(null);
            profile.setCampus(null);
            profile.setContact(null);
            profile.setContactVisible(false);
            userProfileMapper.updateById(profile);
        }
    }

    @Override
    public List<UserReviewItemVO> getUserReviews(Long userId) {
        ensureUserExists(userId);
        return reviewMapper.selectList(
                        new LambdaQueryWrapper<Review>()
                                .eq(Review::getRevieweeId, userId)
                                .orderByDesc(Review::getCreatedAt)
                ).stream()
                .map(review -> UserReviewItemVO.builder()
                        .reviewId(review.getId())
                        .orderId(review.getOrderId())
                        .reviewerId(review.getReviewerId())
                        .reviewerNickname(findNickname(review.getReviewerId()))
                        .rating(review.getRating())
                        .content(review.getContent())
                        .createdAt(review.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public UserCreditVO getUserCredit(Long userId) {
        ensureUserExists(userId);
        UserProfileVO.CreditSummary creditSummary = buildCreditSummary(userId);

        List<UserCreditVO.CreditChangeItem> recentChanges = creditLogMapper.selectList(
                        new LambdaQueryWrapper<CreditLog>()
                                .eq(CreditLog::getUserId, userId)
                                .orderByDesc(CreditLog::getCreatedAt)
                                .last("LIMIT 10"))
                .stream()
                .map(log -> UserCreditVO.CreditChangeItem.builder()
                        .changeAmount(log.getChangeAmount())
                        .scoreBefore(log.getScoreBefore())
                        .scoreAfter(log.getScoreAfter())
                        .reason(log.getReason())
                        .relatedOrderId(log.getRelatedOrderId())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();

        return UserCreditVO.builder()
                .userId(userId)
                .score(creditSummary.getScore())
                .completedOrders(creditSummary.getCompletedOrders())
                .praiseRate(creditSummary.getPraiseRate())
                .recentChanges(recentChanges)
                .build();
    }

    private UserProfileVO buildProfileResponse(User user) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));
        UserProfileVO.CreditSummary creditSummary = buildCreditSummary(user.getId());

        return UserProfileVO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().getValue())
                .status(user.getStatus().getValue())
                .verified(user.getVerified())
                .profile(UserProfileVO.ProfileDetail.builder()
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
                .credit(creditSummary)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserProfileVO.CreditSummary buildCreditSummary(Long userId) {
        CreditLog latestCredit = creditLogMapper.selectOne(
                new LambdaQueryWrapper<CreditLog>()
                        .eq(CreditLog::getUserId, userId)
                        .orderByDesc(CreditLog::getCreatedAt)
                        .last("LIMIT 1"));
        int score = clampCreditScore(latestCredit != null ? latestCredit.getScoreAfter() : DEFAULT_CREDIT_SCORE);

        long completedOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .and(wrapper -> wrapper
                                .eq(Order::getPublisherId, userId)
                                .or()
                                .eq(Order::getServiceProviderId, userId))
                        .in(Order::getStatus, List.of(OrderStatus.COMPLETED, OrderStatus.REVIEWED)));

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreatedAt));
        double praiseRate = reviews.isEmpty()
                ? 1.0
                : reviews.stream().mapToInt(Review::getRating).average().orElse(5.0) / 5.0;

        return UserProfileVO.CreditSummary.builder()
                .score(score)
                .completedOrders((int) completedOrders)
                .praiseRate(praiseRate)
                .build();
    }

    private void ensureUserExists(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private int clampCreditScore(int score) {
        return Math.max(MIN_CREDIT_SCORE, Math.min(MAX_CREDIT_SCORE, score));
    }

    private String findNickname(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .last("LIMIT 1"));
        return profile != null ? profile.getNickname() : "CampusHub User";
    }
}
