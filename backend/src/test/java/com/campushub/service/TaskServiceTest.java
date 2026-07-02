package com.campushub.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.entity.Application;
import com.campushub.entity.Task;
import com.campushub.enums.ApplicationStatus;
import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import com.campushub.mapper.ApplicationMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.FavoriteMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.OrderStatusLogMapper;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.TaskFileMapper;
import com.campushub.mapper.TaskImageMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskImageMapper taskImageMapper;
    @Mock private TaskFileMapper taskFileMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderStatusLogMapper orderStatusLogMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private CreditLogMapper creditLogMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private FileService fileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TaskService taskService;

    @Test
    void shouldRejectInvalidCategoryInsteadOfReturningServerError() {
        taskService = new TaskService(
                taskMapper,
                taskImageMapper,
                taskFileMapper,
                applicationMapper,
                orderMapper,
                orderStatusLogMapper,
                userProfileMapper,
                favoriteMapper,
                creditLogMapper,
                reviewMapper,
                userMapper,
                notificationService,
                fileService,
                objectMapper
        );

        assertThatThrownBy(() -> taskService.listTasks(1, 20, "NOT_A_CATEGORY", null, null, null))
                .isInstanceOf(BusinessException.class);

        verify(taskMapper, never()).selectPage(any(), any());
    }

    @Test
    void shouldExpireOpenTasksBeforeListingTasks() {
        Page<Task> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(taskMapper.selectPage(any(), any())).thenReturn(emptyPage);

        taskService.listTasks(1, 20, null, null, null, null);

        var inOrder = inOrder(taskMapper, orderMapper);
        inOrder.verify(taskMapper).expireOpenTasksPastDeadline();
        inOrder.verify(orderMapper).timeoutPendingConfirmOrdersForExpiredTasks();
        inOrder.verify(orderMapper).timeoutInProgressOrdersPastTaskDeadline();
        inOrder.verify(taskMapper).expireInProgressTasksWithTimedOutOrders();
        inOrder.verify(taskMapper).selectPage(any(), any());
    }

    @Test
    void shouldRejectApplyingExpiredOpenTask() {
        Task task = openTaskPastDeadline();
        when(taskMapper.selectById(100L)).thenReturn(task);
        when(taskMapper.updateStatusIfCurrent(100L, TaskStatus.OPEN.name(), TaskStatus.EXPIRED.name()))
                .thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserId).thenReturn(200L);

            assertThatThrownBy(() -> taskService.applyTask(100L))
                    .isInstanceOf(BusinessException.class);
        }

        verify(taskMapper).updateStatusIfCurrent(100L, TaskStatus.OPEN.name(), TaskStatus.EXPIRED.name());
        verify(orderMapper).timeoutPendingConfirmOrderByTaskId(100L);
        verify(applicationMapper, never()).insert(any());
    }

    @Test
    void shouldRejectConfirmingApplicationAfterTaskDeadline() {
        Application application = new Application();
        application.setId(500L);
        application.setTaskId(100L);
        application.setApplicantId(200L);
        application.setStatus(ApplicationStatus.PENDING);

        Task task = openTaskPastDeadline();
        when(applicationMapper.selectById(500L)).thenReturn(application);
        when(taskMapper.selectById(100L)).thenReturn(task);
        when(taskMapper.updateStatusIfCurrent(100L, TaskStatus.OPEN.name(), TaskStatus.EXPIRED.name()))
                .thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserId).thenReturn(10L);

            assertThatThrownBy(() -> taskService.confirmApplication(500L))
                    .isInstanceOf(BusinessException.class);
        }

        verify(taskMapper).updateStatusIfCurrent(100L, TaskStatus.OPEN.name(), TaskStatus.EXPIRED.name());
        verify(orderMapper).timeoutPendingConfirmOrderByTaskId(100L);
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void shouldNotExpireCompletedTaskPastDeadline() {
        Task task = openTaskPastDeadline();
        task.setStatus(TaskStatus.COMPLETED);

        when(taskMapper.selectById(100L)).thenReturn(task);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserId).thenReturn(200L);

            assertThatThrownBy(() -> taskService.applyTask(100L))
                    .isInstanceOf(BusinessException.class);
        }

        verify(taskMapper, never()).updateStatusIfCurrent(100L, TaskStatus.OPEN.name(), TaskStatus.EXPIRED.name());
        verify(orderMapper, never()).timeoutPendingConfirmOrderByTaskId(100L);
    }

    @Test
    void shouldTimeoutInProgressOrderAfterTaskDeadline() {
        Task task = openTaskPastDeadline();
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskMapper.selectById(100L)).thenReturn(task);
        when(orderMapper.timeoutInProgressOrderByTaskId(100L)).thenReturn(1);
        when(taskMapper.updateStatusIfCurrent(100L, TaskStatus.IN_PROGRESS.name(), TaskStatus.EXPIRED.name()))
                .thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserId).thenReturn(200L);

            assertThatThrownBy(() -> taskService.applyTask(100L))
                    .isInstanceOf(BusinessException.class);
        }

        verify(orderMapper).timeoutInProgressOrderByTaskId(100L);
        verify(taskMapper).updateStatusIfCurrent(100L, TaskStatus.IN_PROGRESS.name(), TaskStatus.EXPIRED.name());
    }

    private Task openTaskPastDeadline() {
        Task task = new Task();
        task.setId(100L);
        task.setPublisherId(10L);
        task.setCategory(TaskCategory.EXPRESS);
        task.setTitle("帮忙取快递");
        task.setDescription("取一个包裹");
        task.setCampus("仙林");
        task.setRewardType(RewardType.CASH);
        task.setDeadline(LocalDateTime.now().minusMinutes(1));
        task.setStatus(TaskStatus.OPEN);
        task.setAnonymous(false);
        return task;
    }
}
