package com.campushub.service;

import com.campushub.entity.CreditLog;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.UserRole;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.service.impl.UserServiceImpl;
import com.campushub.vo.user.PublicProfileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private FileService fileService;
    @Mock private ReviewMapper reviewMapper;
    @Mock private CreditLogMapper creditLogMapper;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldClampPublicProfileCreditScoreWhenStoredScoreIsOutOfRange() {
        User user = new User();
        user.setId(10001L);
        user.setEmail("student.demo@smail.nju.edu.cn");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        when(userMapper.selectById(10001L)).thenReturn(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(10001L);
        profile.setNickname("Demo");
        profile.setContactVisible(false);
        when(userProfileMapper.selectOne(any())).thenReturn(profile);

        CreditLog latest = new CreditLog();
        latest.setScoreAfter(505);
        when(creditLogMapper.selectOne(any())).thenReturn(latest);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.selectList(any())).thenReturn(List.of());

        PublicProfileVO result = userService.getPublicProfile(10001L);

        assertThat(result.getCreditScore()).isEqualTo(100);
    }
}
