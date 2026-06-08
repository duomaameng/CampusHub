package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.mapper.ApplicationMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.FavoriteMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.OrderStatusLogMapper;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.TaskImageMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskImageMapper taskImageMapper;
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
}
