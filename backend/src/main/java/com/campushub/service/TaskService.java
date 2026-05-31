package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.dto.task.TaskApplyRequest;
import com.campushub.dto.task.TaskCreateRequest;
import com.campushub.dto.task.TaskUpdateRequest;
import com.campushub.entity.Application;
import com.campushub.entity.CreditLog;
import com.campushub.entity.Favorite;
import com.campushub.entity.FileRecord;
import com.campushub.entity.Order;
import com.campushub.entity.OrderStatusLog;
import com.campushub.entity.Task;
import com.campushub.entity.TaskImage;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.ApplicationStatus;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.ApplicationMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.FavoriteMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.OrderStatusLogMapper;
import com.campushub.mapper.TaskImageMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.task.ApplicationConfirmVO;
import com.campushub.vo.task.ApplicationItemVO;
import com.campushub.vo.task.FavoriteToggleVO;
import com.campushub.vo.task.TaskApplyVO;
import com.campushub.vo.task.TaskCreateVO;
import com.campushub.vo.task.TaskItemVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskImageMapper taskImageMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final UserProfileMapper userProfileMapper;
    private final FavoriteMapper favoriteMapper;
    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public PageResult<TaskItemVO> listTasks(int page, int size, String category, String campus, String keyword, String sort) {
        Page<Task> pageQuery = new Page<>(page, size);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .in(Task::getStatus, Arrays.asList(TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED))
                .orderByDesc(Task::getCreatedAt);

        if (category != null && !category.isBlank()) {
            wrapper.eq(Task::getCategory, parseTaskCategory(category));
        }
        if (campus != null && !campus.isBlank()) {
            wrapper.eq(Task::getCampus, campus);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Task::getTitle, keyword).or().like(Task::getDescription, keyword));
        }
        if ("deadline".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc(Task::getDeadline);
        } else {
            wrapper.orderByDesc(Task::getCreatedAt);
        }

        Page<Task> result = taskMapper.selectPage(pageQuery, wrapper);
        List<TaskItemVO> records = result.getRecords().stream().map(this::toTaskItemVO).toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    public TaskItemVO getTask(Long taskId) {
        return toTaskItemVO(requireTask(taskId));
    }

    @Transactional
    public TaskCreateVO createTask(TaskCreateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(currentUser.getVerified())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OPERATION);
        }

        Task task = new Task();
        task.setPublisherId(currentUserId);
        task.setCategory(request.getCategory());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription().trim());
        task.setCampus(request.getCampus().trim());
        task.setRewardType(request.getRewardType());
        task.setDeadline(request.getDeadline());
        task.setStatus(TaskStatus.OPEN);
        task.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));
        task.setCategoryFields(toJson(request.getCategoryFields()));
        taskMapper.insert(task);

        bindTaskImages(task.getId(), request.getImageIds());
        return new TaskCreateVO(task.getId(), task.getStatus(), task.getCreatedAt());
    }

    @Transactional
    public TaskApplyVO applyTask(Long taskId, TaskApplyRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = requireTask(taskId);

        if (!TaskStatus.OPEN.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        if (Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_APPLY_OWN_TASK);
        }

        long duplicated = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, taskId)
                        .eq(Application::getApplicantId, currentUserId)
                        .in(Application::getStatus, ApplicationStatus.PENDING, ApplicationStatus.APPROVED)
        );
        if (duplicated > 0) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        Application application = new Application();
        application.setTaskId(taskId);
        application.setApplicantId(currentUserId);
        application.setMessage(request.getMessage().trim());
        application.setStatus(ApplicationStatus.PENDING);
        applicationMapper.insert(application);

        String applicantNickname = Optional.ofNullable(findProfile(currentUserId))
                .map(UserProfile::getNickname)
                .orElse("CampusHub User");
        notificationService.createApplicationNotification(task.getPublisherId(), task.getId(), applicantNickname, task.getTitle());

        return new TaskApplyVO(application.getId(), taskId, application.getStatus(), application.getCreatedAt());
    }

    public List<ApplicationItemVO> listApplications(Long taskId) {
        Task task = requireTask(taskId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }

        return applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, taskId)
                        .orderByDesc(Application::getCreatedAt))
                .stream()
                .map(this::toApplicationItemVO)
                .toList();
    }

    @Transactional
    public ApplicationConfirmVO confirmApplication(Long applicationId) {
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        if (!ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        Task task = requireTask(application.getTaskId());
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (!TaskStatus.OPEN.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_TAKEN);
        }
        int updatedRows = taskMapper.updateStatusIfCurrent(task.getId(), TaskStatus.OPEN.name(), TaskStatus.IN_PROGRESS.name());
        if (updatedRows != 1) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_TAKEN);
        }

        int applicationUpdatedRows = applicationMapper.update(null, new LambdaUpdateWrapper<Application>()
                .eq(Application::getId, applicationId)
                .eq(Application::getStatus, ApplicationStatus.PENDING)
                .set(Application::getStatus, ApplicationStatus.APPROVED));
        if (applicationUpdatedRows != 1) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
        application.setStatus(ApplicationStatus.APPROVED);

        applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, task.getId())
                        .eq(Application::getStatus, ApplicationStatus.PENDING))
                .forEach(item -> {
                    item.setStatus(ApplicationStatus.REJECTED);
                    applicationMapper.updateById(item);
                });

        task.setStatus(TaskStatus.IN_PROGRESS);

        Order order = new Order();
        order.setTaskId(task.getId());
        order.setPublisherId(task.getPublisherId());
        order.setServiceProviderId(application.getApplicantId());
        order.setStatus(OrderStatus.IN_PROGRESS);
        orderMapper.insert(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus(OrderStatus.PENDING_CONFIRM.name());
        log.setToStatus(OrderStatus.IN_PROGRESS.name());
        log.setOperatorId(currentUserId);
        log.setReason("Publisher confirmed application");
        orderStatusLogMapper.insert(log);

        notificationService.createOrderStatusNotification(order.getServiceProviderId(), order.getId(), OrderStatus.IN_PROGRESS);
        return new ApplicationConfirmVO(order.getId(), task.getId(), order.getStatus(), order.getCreatedAt());
    }

    @Transactional
    public TaskItemVO updateTask(Long taskId, TaskUpdateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = requireTask(taskId);
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (!TaskStatus.OPEN.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        ensureNoApplications(taskId);

        if (request.getCategory() != null) {
            task.setCategory(request.getCategory());
        }
        if (request.getTitle() != null) {
            task.setTitle(requireText(request.getTitle(), "title"));
        }
        if (request.getDescription() != null) {
            task.setDescription(requireText(request.getDescription(), "description"));
        }
        if (request.getCampus() != null) {
            task.setCampus(requireText(request.getCampus(), "campus"));
        }
        if (request.getRewardType() != null) {
            task.setRewardType(request.getRewardType());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getAnonymous() != null) {
            task.setAnonymous(request.getAnonymous());
        }
        if (request.getCategoryFields() != null) {
            task.setCategoryFields(toJson(request.getCategoryFields()));
        }
        taskMapper.updateById(task);

        if (request.getImageIds() != null) {
            taskImageMapper.delete(new LambdaQueryWrapper<TaskImage>().eq(TaskImage::getTaskId, taskId));
            bindTaskImages(taskId, request.getImageIds());
        }
        return toTaskItemVO(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = requireTask(taskId);
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (!TaskStatus.OPEN.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        ensureNoApplications(taskId);
        taskMapper.deleteById(taskId);
    }

    @Transactional
    public FavoriteToggleVO toggleFavorite(Long taskId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        requireTask(taskId);
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, currentUserId)
                .eq(Favorite::getTaskId, taskId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return new FavoriteToggleVO(false);
        } else {
            Favorite fav = new Favorite();
            fav.setUserId(currentUserId);
            fav.setTaskId(taskId);
            try {
                favoriteMapper.insert(fav);
            } catch (DuplicateKeyException ignored) {
                return new FavoriteToggleVO(true);
            }
            return new FavoriteToggleVO(true);
        }
    }

    public PageResult<TaskItemVO> getFavorites(int page, int size) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Page<Favorite> favPage = favoriteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, currentUserId)
                        .orderByDesc(Favorite::getCreatedAt));

        if (favPage.getRecords().isEmpty()) {
            return PageResult.of(0, page, size, List.of());
        }

        Set<Long> taskIds = favPage.getRecords().stream().map(Favorite::getTaskId).collect(java.util.stream.Collectors.toSet());
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>().in(Task::getId, taskIds));
        Map<Long, Task> taskMap = tasks.stream().collect(java.util.stream.Collectors.toMap(Task::getId, t -> t));

        List<TaskItemVO> records = favPage.getRecords().stream()
                .map(fav -> {
                    Task task = taskMap.get(fav.getTaskId());
                    if (task == null) return null;
                    TaskItemVO vo = toTaskItemVO(task);
                    vo.setFavorited(true);
                    return vo;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return PageResult.of(favPage.getTotal(), page, size, records);
    }

    @Transactional
    public void rejectApplication(Long applicationId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        Task task = requireTask(application.getTaskId());
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (!ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
        int updatedRows = applicationMapper.update(null, new LambdaUpdateWrapper<Application>()
                .eq(Application::getId, applicationId)
                .eq(Application::getStatus, ApplicationStatus.PENDING)
                .set(Application::getStatus, ApplicationStatus.REJECTED));
        if (updatedRows != 1) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private void ensureNoApplications(Long taskId) {
        long applicationCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>().eq(Application::getTaskId, taskId));
        if (applicationCount > 0) {
            throw new BusinessException(ErrorCode.TASK_HAS_APPLICATION);
        }
    }

    private void bindTaskImages(Long taskId, List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        int index = 0;
        for (Long imageId : imageIds) {
            FileRecord fileRecord = fileService.requireOwnedFile(imageId, UploadBusinessType.TASK_IMAGE);
            TaskImage image = new TaskImage();
            image.setTaskId(taskId);
            image.setImageUrl(fileRecord.getFileUrl());
            image.setSortOrder(index++);
            taskImageMapper.insert(image);
        }
    }

    private TaskItemVO toTaskItemVO(Task task) {
        TaskItemVO vo = new TaskItemVO();
        vo.setId(task.getId());
        vo.setPublisherId(task.getPublisherId());

        UserProfile publisherProfile = findProfile(task.getPublisherId());
        vo.setPublisherNickname(publisherProfile != null ? publisherProfile.getNickname() : "CampusHub User");
        vo.setPublisherAvatarUrl(publisherProfile != null ? publisherProfile.getAvatarUrl() : null);

        vo.setCategory(task.getCategory());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setCampus(task.getCampus());
        vo.setRewardType(task.getRewardType());
        vo.setDeadline(task.getDeadline());
        vo.setStatus(task.getStatus());
        vo.setAnonymous(task.getAnonymous());
        vo.setImageUrls(taskImageMapper.selectList(new LambdaQueryWrapper<TaskImage>()
                        .eq(TaskImage::getTaskId, task.getId())
                        .orderByAsc(TaskImage::getSortOrder))
                .stream()
                .map(TaskImage::getImageUrl)
                .toList());
        vo.setApplicationCount(applicationMapper.selectCount(new LambdaQueryWrapper<Application>().eq(Application::getTaskId, task.getId())));
        vo.setFavoriteCount(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>().eq(Favorite::getTaskId, task.getId())));
        vo.setFavorited(false);
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        vo.setCategoryFields(parseCategoryFields(task.getCategoryFields()));
        return vo;
    }

    private ApplicationItemVO toApplicationItemVO(Application application) {
        ApplicationItemVO vo = new ApplicationItemVO();
        vo.setId(application.getId());
        vo.setTaskId(application.getTaskId());
        vo.setApplicantId(application.getApplicantId());

        UserProfile profile = findProfile(application.getApplicantId());
        vo.setApplicantNickname(profile != null ? profile.getNickname() : "CampusHub User");
        vo.setApplicantAvatarUrl(profile != null ? profile.getAvatarUrl() : null);
        vo.setApplicantCreditScore(findLatestCreditScore(application.getApplicantId()));
        vo.setMessage(application.getMessage());
        vo.setStatus(application.getStatus());
        vo.setCreatedAt(application.getCreatedAt());
        return vo;
    }

    private Task requireTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private UserProfile findProfile(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
    }

    private Integer findLatestCreditScore(Long userId) {
        CreditLog latest = creditLogMapper.selectOne(new LambdaQueryWrapper<CreditLog>()
                .eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getId)
                .last("LIMIT 1"));
        return latest != null ? latest.getScoreAfter() : 100;
    }

    private String toJson(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid categoryFields format");
        }
    }

    private Map<String, Object> parseCategoryFields(String categoryFields) {
        if (categoryFields == null || categoryFields.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(categoryFields, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("raw", categoryFields);
        }
    }

    private TaskCategory parseTaskCategory(String category) {
        try {
            return TaskCategory.valueOf(category.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid task category");
        }
    }
}
