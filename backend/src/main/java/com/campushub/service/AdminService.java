package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.entity.AdminOperationLog;
import com.campushub.entity.CreditLog;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.AdminOperationLogMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.admin.AdminUserItemVO;
import com.campushub.vo.admin.AdminUserStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final CreditLogMapper creditLogMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public PageResult<AdminUserItemVO> listUsers(int page, int size, String keyword, UserStatus status, Boolean verified) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String trimmedKeyword = keyword == null ? null : keyword.trim();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (verified != null) {
            wrapper.eq(User::getVerified, verified);
        }
        if (StringUtils.hasText(trimmedKeyword)) {
            List<Long> matchedUserIds = userProfileMapper.selectList(
                            new LambdaQueryWrapper<UserProfile>()
                                    .like(UserProfile::getNickname, trimmedKeyword)
                    )
                    .stream()
                    .map(UserProfile::getUserId)
                    .toList();
            wrapper.and(w -> {
                w.like(User::getEmail, trimmedKeyword);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in(User::getId, matchedUserIds);
                }
            });
        }

        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<Long> userIds = result.getRecords().stream().map(User::getId).toList();
        Map<Long, UserProfile> profilesByUserId = loadProfiles(userIds);

        List<AdminUserItemVO> records = result.getRecords().stream()
                .map(user -> toAdminUserItemVO(user, profilesByUserId.get(user.getId())))
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Transactional
    public AdminUserStatusVO updateUserStatus(Long userId, UserStatus status, String reason) {
        if (status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "User status is required");
        }
        if (UserStatus.ANONYMIZED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Admin user status update only supports ACTIVE or DISABLED");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        if (UserStatus.ACTIVE.equals(status)) {
            user.setLoginFailures(0);
            user.setLockedUntil(null);
        }
        userMapper.updateById(user);

        recordUserStatusOperation(userId, status, reason);
        return new AdminUserStatusVO(userId, status);
    }

    private int normalizePage(int page) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Size must be greater than or equal to 1");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, UserProfile> loadProfiles(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileMapper.selectList(
                        new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds)
                )
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (left, right) -> left));
    }

    private AdminUserItemVO toAdminUserItemVO(User user, UserProfile profile) {
        return new AdminUserItemVO(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getNickname() : "",
                user.getRole(),
                user.getStatus(),
                user.getVerified(),
                findLatestCreditScore(user.getId()),
                user.getCreatedAt()
        );
    }

    private Integer findLatestCreditScore(Long userId) {
        CreditLog latest = creditLogMapper.selectOne(new LambdaQueryWrapper<CreditLog>()
                .eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getId)
                .last("LIMIT 1"));
        return latest != null ? latest.getScoreAfter() : 100;
    }

    private void recordUserStatusOperation(Long userId, UserStatus status, String reason) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(SecurityUtils.requireCurrentUserId());
        log.setOperationType(UserStatus.DISABLED.equals(status) ? "DISABLE_USER" : "ENABLE_USER");
        log.setTargetType("USER");
        log.setTargetId(userId);
        log.setDetail(buildStatusOperationDetail(status, reason));
        adminOperationLogMapper.insert(log);
    }

    private String buildStatusOperationDetail(UserStatus status, String reason) {
        String detail = "Set user status to " + status.getValue();
        if (StringUtils.hasText(reason)) {
            detail += ". Reason: " + reason.trim();
        }
        return detail;
    }
}
