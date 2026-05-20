package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.response.ApplicationResponse;
import com.campushub.entity.Application;
import com.campushub.entity.Task;
import com.campushub.entity.UserProfile;
import com.campushub.enums.ApplicationStatus;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.TaskStatus;
import com.campushub.mapper.*;
import com.campushub.security.SecurityUtils;
import com.campushub.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final TaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public List<ApplicationResponse> listApplications(Long taskId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看自己发布需求的接单申请");
        }

        List<Application> applications = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, taskId)
                        .orderByDesc(Application::getCreatedAt));

        return applications.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplicationResponse apply(Long taskId, String message) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (task.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_APPLY_OWN_TASK);
        }
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }

        Long existingCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, taskId)
                        .eq(Application::getApplicantId, userId)
                        .eq(Application::getStatus, ApplicationStatus.PENDING));
        if (existingCount > 0) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        Application application = new Application();
        application.setTaskId(taskId);
        application.setApplicantId(userId);
        application.setMessage(message);
        application.setStatus(ApplicationStatus.PENDING);
        applicationMapper.insert(application);

        return toResponse(application);
    }

    @Override
    @Transactional
    public void confirm(Long applicationId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        Task task = taskMapper.selectById(application.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有发布者可以确认接单");
        }
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_TAKEN);
        }

        // Use optimistic locking to prevent race conditions
        task.setStatus(TaskStatus.IN_PROGRESS);
        int updated = taskMapper.update(task,
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getId, task.getId())
                        .eq(Task::getVersion, task.getVersion()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_TAKEN);
        }

        application.setStatus(ApplicationStatus.APPROVED);
        applicationMapper.updateById(application);

        // Reject all other pending applications
        List<Application> others = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, task.getId())
                        .eq(Application::getStatus, ApplicationStatus.PENDING)
                        .ne(Application::getId, application.getId()));
        for (Application other : others) {
            other.setStatus(ApplicationStatus.REJECTED);
            applicationMapper.updateById(other);
        }

        // Create order
        com.campushub.entity.Order order = new com.campushub.entity.Order();
        order.setTaskId(task.getId());
        order.setPublisherId(task.getPublisherId());
        order.setServiceProviderId(application.getApplicantId());
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setVersion(0);
        orderMapper.insert(order);
    }

    @Override
    @Transactional
    public void reject(Long applicationId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }

        Task task = taskMapper.selectById(application.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有发布者可以拒绝接单");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        applicationMapper.updateById(application);
    }

    private ApplicationResponse toResponse(Application app) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, app.getApplicantId()));

        return ApplicationResponse.builder()
                .id(app.getId())
                .taskId(app.getTaskId())
                .applicantId(app.getApplicantId())
                .applicantNickname(profile != null ? profile.getNickname() : "未知用户")
                .applicantCreditScore(100)
                .message(app.getMessage())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
