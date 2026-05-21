package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.request.*;
import com.campushub.dto.response.LoginResponse;
import com.campushub.dto.response.RegisterResponse;
import com.campushub.dto.response.VerifyEmailResponse;
import com.campushub.dto.response.UserProfileResponse;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.entity.VerificationCode;
import com.campushub.enums.UserRole;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.mapper.VerificationCodeMapper;
import com.campushub.security.JwtTokenProvider;
import com.campushub.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.getEmail().endsWith("@smail.nju.edu.cn")
                && !request.getEmail().endsWith("@nju.edu.cn")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请使用学校邮箱注册");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerified(false);
        user.setLoginFailures(0);
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(request.getEmail().split("@")[0]);
        profile.setContactVisible(false);
        userProfileMapper.insert(profile);

        log.info("User registered: {}", user.getEmail());
        return RegisterResponse.builder().userId(user.getId()).email(user.getEmail()).build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setLoginFailures(user.getLoginFailures() + 1);
            if (user.getLoginFailures() >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            userMapper.updateById(user);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        user.setLoginFailures(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().getValue());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(LoginResponse.LoginUser.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole().getValue())
                        .status(user.getStatus().getValue())
                        .verified(user.getVerified())
                        .nickname(profile != null ? profile.getNickname() : "")
                        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                        .build())
                .build();
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String purpose = request.getPurpose();
        if (!"REGISTER".equals(purpose) && !"RESET_PASSWORD".equals(purpose)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码用途无效");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if ("REGISTER".equals(purpose) && user != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if ("RESET_PASSWORD".equals(purpose) && user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }

        String code = String.format("%06d", new Random().nextInt(1000000));

        VerificationCode vc = new VerificationCode();
        vc.setEmail(request.getEmail());
        vc.setCode(code);
        vc.setPurpose(purpose);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        vc.setUsed(false);
        verificationCodeMapper.insert(vc);

        log.info("Verification code sent to {}: {}", request.getEmail(), code);
    }

    @Override
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }
        if (user.getVerified()) {
            return new VerifyEmailResponse(true);
        }

        VerificationCode vc = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getEmail, request.getEmail())
                        .eq(VerificationCode::getPurpose, "REGISTER")
                        .eq(VerificationCode::getUsed, false)
                        .orderByDesc(VerificationCode::getCreatedAt)
                        .last("LIMIT 1"));
        if (vc == null || !vc.getCode().equals(request.getCode().trim())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        if (vc.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        vc.setUsed(true);
        verificationCodeMapper.updateById(vc);

        user.setVerified(true);
        userMapper.updateById(user);

        log.info("Email verified: {}", request.getEmail());
        return new VerifyEmailResponse(true);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的新密码不一致");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }

        VerificationCode vc = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getEmail, request.getEmail())
                        .eq(VerificationCode::getPurpose, "RESET_PASSWORD")
                        .eq(VerificationCode::getUsed, false)
                        .orderByDesc(VerificationCode::getCreatedAt)
                        .last("LIMIT 1"));
        if (vc == null || !vc.getCode().equals(request.getCode().trim())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        if (vc.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        vc.setUsed(true);
        verificationCodeMapper.updateById(vc);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setLoginFailures(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        log.info("Password reset for: {}", request.getEmail());
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
