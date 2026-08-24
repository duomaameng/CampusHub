package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.dto.request.SendVerificationCodeRequest;
import com.campushub.entity.User;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.mapper.VerificationCodeMapper;
import com.campushub.security.JwtTokenProvider;
import com.campushub.security.TokenBlacklistService;
import com.campushub.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private VerificationCodeMapper verificationCodeMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldRejectRegisterVerificationCodeForExistingEmail() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("student.demo@smail.nju.edu.cn");
        request.setPurpose("REGISTER");

        User existing = new User();
        existing.setId(1L);
        existing.setEmail(request.getEmail());
        existing.setStatus(UserStatus.ACTIVE);
        when(userMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> authService.sendVerificationCode(request))
                .isInstanceOf(BusinessException.class);

        verify(verificationCodeMapper, never()).insert(any());
        verify(emailService, never()).sendVerificationCode(any(), any(), any());
    }
}
