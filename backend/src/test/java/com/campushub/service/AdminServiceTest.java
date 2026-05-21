package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.entity.AdminOperationLog;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.UserRole;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.AdminOperationLogMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.admin.AdminUserItemVO;
import com.campushub.vo.admin.AdminUserStatusVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private CreditLogMapper creditLogMapper;
    @Mock private AdminOperationLogMapper adminOperationLogMapper;

    private AdminService adminService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userMapper, userProfileMapper, creditLogMapper, adminOperationLogMapper);
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void shouldListUsersWithProfileAndDefaultCreditScore() {
        User user = createUser(2L, "student@smail.nju.edu.cn", UserRole.STUDENT, UserStatus.ACTIVE);
        Page<User> userPage = new Page<>(1, 20);
        userPage.setRecords(List.of(user));
        userPage.setTotal(1);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(userPage);

        UserProfile profile = new UserProfile();
        profile.setUserId(2L);
        profile.setNickname("Student");
        when(userProfileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(profile));
        when(creditLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        PageResult<AdminUserItemVO> result = adminService.listUsers(1, 20, null, null, null);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getEmail()).isEqualTo("student@smail.nju.edu.cn");
        assertThat(result.getRecords().get(0).getNickname()).isEqualTo("Student");
        assertThat(result.getRecords().get(0).getCreditScore()).isEqualTo(100);
    }

    @Test
    void shouldUpdateUserStatusAndWriteAuditLog() {
        User user = createUser(2L, "student@smail.nju.edu.cn", UserRole.STUDENT, UserStatus.ACTIVE);
        user.setLoginFailures(3);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminUserStatusVO result = adminService.updateUserStatus(2L, UserStatus.DISABLED, "spam");

        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        verify(userMapper).updateById(user);

        ArgumentCaptor<AdminOperationLog> captor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(adminOperationLogMapper).insert(captor.capture());
        AdminOperationLog log = captor.getValue();
        assertThat(log.getAdminId()).isEqualTo(1L);
        assertThat(log.getOperationType()).isEqualTo("DISABLE_USER");
        assertThat(log.getTargetType()).isEqualTo("USER");
        assertThat(log.getTargetId()).isEqualTo(2L);
        assertThat(log.getDetail()).contains("spam");
    }

    @Test
    void shouldClearLoginLockWhenEnablingUser() {
        User user = createUser(2L, "student@smail.nju.edu.cn", UserRole.STUDENT, UserStatus.DISABLED);
        user.setLoginFailures(5);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(userMapper.selectById(2L)).thenReturn(user);

        adminService.updateUserStatus(2L, UserStatus.ACTIVE, null);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getLoginFailures()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void shouldRejectAnonymizedStatusForAdminStatusUpdate() {
        assertThatThrownBy(() -> adminService.updateUserStatus(2L, UserStatus.ANONYMIZED, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ACTIVE or DISABLED");
    }

    private User createUser(Long id, String email, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        return user;
    }
}
