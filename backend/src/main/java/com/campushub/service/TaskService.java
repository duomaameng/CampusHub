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
import com.campushub.entity.Review;
import com.campushub.entity.Task;
import com.campushub.entity.TaskImage;
import com.campushub.entity.TaskFile;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.ApplicationStatus;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.RewardPaymentMethod;
import com.campushub.enums.RewardType;
import com.campushub.enums.TaskCategory;
import com.campushub.enums.TaskStatus;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.ApplicationMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.FavoriteMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.OrderStatusLogMapper;
import com.campushub.mapper.ReviewMapper;
import com.campushub.mapper.TaskImageMapper;
import com.campushub.mapper.TaskFileMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.realtime.RealtimeEventPublisher;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.task.ApplicationConfirmVO;
import com.campushub.vo.task.ApplicationItemVO;
import com.campushub.vo.task.FavoriteToggleVO;
import com.campushub.vo.task.TaskApplyVO;
import com.campushub.vo.task.TaskCreateVO;
import com.campushub.vo.task.TaskItemVO;
import com.campushub.vo.task.TaskFileVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    private static final int DEFAULT_CREDIT_SCORE = 100;
    private static final int MIN_CREDIT_SCORE = 0;
    private static final int MAX_CREDIT_SCORE = 100;
    private static final Map<TaskCategory, Set<String>> PRIVATE_CATEGORY_FIELDS = Map.of(
            TaskCategory.EXPRESS, Set.of("pickupCode", "deliveryLocation", "deliveryAddress"),
            TaskCategory.LOST_FOUND, Set.of("contactInfo")
    );

    private final TaskMapper taskMapper;
    private final TaskImageMapper taskImageMapper;
    private final TaskFileMapper taskFileMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final UserProfileMapper userProfileMapper;
    private final FavoriteMapper favoriteMapper;
    private final CreditLogMapper creditLogMapper;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public PageResult<TaskItemVO> listTasks(int page, int size, String category, String campus, String keyword, String sort) {
        expireOpenTasksPastDeadline();

        Page<Task> pageQuery = new Page<>(page, size);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .in(Task::getStatus, Arrays.asList(TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED));

        if (category != null && !category.isBlank()) {
            wrapper.eq(Task::getCategory, parseTaskCategory(category));
        }
        if (campus != null && !campus.isBlank()) {
            wrapper.eq(Task::getCampus, campus);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Task::getTitle, keyword).or().like(Task::getDescription, keyword));
        }

        String orderBy = "FIELD(status, 'OPEN', 'IN_PROGRESS', 'COMPLETED')";
        if ("deadline".equalsIgnoreCase(sort)) {
            orderBy += ", deadline ASC";
        } else {
            orderBy += ", created_at DESC";
        }
        wrapper.last("ORDER BY " + orderBy);

        Page<Task> result = taskMapper.selectPage(pageQuery, wrapper);
        List<TaskItemVO> records = result.getRecords().stream().map(this::toTaskItemVO).toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    public TaskItemVO getTask(Long taskId) {
        TaskItemVO vo = toTaskItemVO(requireFreshTask(taskId));
        vo.setFiles(listTaskFiles(taskId));
        vo.setFileDownloadAllowed(canCurrentUserDownloadTaskFiles(taskId));
        return vo;
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
        applyReward(task, request.getRewardType(), request.getRewardAmount(), request.getPaymentMethod());
        task.setDeadline(request.getDeadline());
        task.setStatus(TaskStatus.OPEN);
        task.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));
        task.setCategoryFields(toJson(normalizeCategoryFields(request.getCategory(), request.getCategoryFields())));
        taskMapper.insert(task);

        bindTaskImages(task.getId(), request.getImageIds());
        bindTaskFiles(task.getId(), request.getFileIds());
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, task.getId());
        return new TaskCreateVO(task.getId(), task.getStatus(), task.getCreatedAt());
    }

    @Transactional
    public TaskApplyVO applyTask(Long taskId, TaskApplyRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = requireFreshTask(taskId);

        if (TaskCategory.TEAM_UP.equals(task.getCategory())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "组队搭子是信息帖，不能申请接单，请通过帖子中的联系方式联系发布者");
        }
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
                .orElse("CampusHub 用户");
        notificationService.createApplicationNotification(task.getPublisherId(), task.getId(), applicantNickname, task.getTitle());
        realtimeEventPublisher.user(currentUserId, RealtimeEventPublisher.TASKS_CHANGED, taskId);

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
    public void markApplicationsViewed(Long taskId) {
        Task task = requireTask(taskId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        applicationMapper.update(null, new LambdaUpdateWrapper<Application>()
                .eq(Application::getTaskId, taskId)
                .eq(Application::getPublisherViewed, false)
                .set(Application::getPublisherViewed, true));
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

        Task task = requireFreshTask(application.getTaskId());
        if (TaskCategory.TEAM_UP.equals(task.getCategory())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "组队搭子是信息帖，不能确认接单申请");
        }
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
        orderMapper.delete(new LambdaQueryWrapper<Order>()
                .eq(Order::getTaskId, task.getId())
                .in(Order::getStatus, OrderStatus.CANCELLED, OrderStatus.PENDING_CONFIRM));
        orderMapper.insert(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus(OrderStatus.PENDING_CONFIRM.name());
        log.setToStatus(OrderStatus.IN_PROGRESS.name());
        log.setOperatorId(currentUserId);
        log.setReason("发布方确认接单申请");
        orderStatusLogMapper.insert(log);

        notificationService.createOrderStatusNotification(order.getServiceProviderId(), order.getId(), OrderStatus.IN_PROGRESS);
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, task.getId());
        realtimeEventPublisher.user(task.getPublisherId(), RealtimeEventPublisher.APPLICATIONS_CHANGED, task.getId());
        publishOrderChange(order);
        return new ApplicationConfirmVO(order.getId(), task.getId(), order.getStatus(), order.getCreatedAt());
    }

    @Transactional
    public TaskItemVO updateTask(Long taskId, TaskUpdateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = requireFreshTask(taskId);
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (!TaskStatus.OPEN.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        if (!TaskCategory.TEAM_UP.equals(task.getCategory())) {
            ensureNoApplications(taskId);
        }

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
        if (request.getRewardType() != null || request.getRewardAmount() != null || request.getPaymentMethod() != null) {
            applyReward(
                    task,
                    task.getRewardType(),
                    request.getRewardAmount() != null ? request.getRewardAmount() : task.getRewardAmount(),
                    request.getPaymentMethod() != null ? request.getPaymentMethod() : task.getPaymentMethod()
            );
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getAnonymous() != null) {
            task.setAnonymous(request.getAnonymous());
        }
        if (request.getCategoryFields() != null) {
            task.setCategoryFields(toJson(normalizeCategoryFields(task.getCategory(), request.getCategoryFields())));
        } else if (TaskCategory.TEAM_UP.equals(task.getCategory())) {
            task.setCategoryFields(toJson(normalizeCategoryFields(task.getCategory(), parseCategoryFields(task.getCategoryFields()))));
        }
        taskMapper.updateById(task);

        if (request.getImageIds() != null) {
            taskImageMapper.delete(new LambdaQueryWrapper<TaskImage>().eq(TaskImage::getTaskId, taskId));
            bindTaskImages(taskId, request.getImageIds());
        }
        if (request.getFileIds() != null) {
            taskFileMapper.delete(new LambdaQueryWrapper<TaskFile>().eq(TaskFile::getTaskId, taskId));
            bindTaskFiles(taskId, request.getFileIds());
        }
        TaskItemVO vo = toTaskItemVO(task);
        vo.setFiles(listTaskFiles(taskId));
        vo.setFileDownloadAllowed(canCurrentUserDownloadTaskFiles(taskId));
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, taskId);
        return vo;
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Task task = requireFreshTask(taskId);
        if (!Objects.equals(task.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (!TaskStatus.OPEN.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        if (!TaskCategory.TEAM_UP.equals(task.getCategory())) {
            ensureNoApplications(taskId);
        }

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getTaskId, taskId));
        for (Order order : orders) {
            reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId()));
            orderMapper.deleteById(order.getId());
        }

        applicationMapper.delete(new LambdaQueryWrapper<Application>().eq(Application::getTaskId, taskId));

        taskFileMapper.delete(new LambdaQueryWrapper<TaskFile>().eq(TaskFile::getTaskId, taskId));

        taskMapper.deleteById(taskId);
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, taskId);
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
            realtimeEventPublisher.user(currentUserId, RealtimeEventPublisher.TASKS_CHANGED, taskId);
            return new FavoriteToggleVO(false);
        } else {
            Favorite fav = new Favorite();
            fav.setUserId(currentUserId);
            fav.setTaskId(taskId);
            try {
                favoriteMapper.insert(fav);
            } catch (DuplicateKeyException ignored) {
                realtimeEventPublisher.user(currentUserId, RealtimeEventPublisher.TASKS_CHANGED, taskId);
                return new FavoriteToggleVO(true);
            }
            realtimeEventPublisher.user(currentUserId, RealtimeEventPublisher.TASKS_CHANGED, taskId);
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
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, task.getId());
        realtimeEventPublisher.user(task.getPublisherId(), RealtimeEventPublisher.APPLICATIONS_CHANGED, task.getId());
        realtimeEventPublisher.user(application.getApplicantId(), RealtimeEventPublisher.TASKS_CHANGED, task.getId());
    }

    private void publishOrderChange(Order order) {
        realtimeEventPublisher.user(order.getPublisherId(), RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
        realtimeEventPublisher.user(order.getServiceProviderId(), RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " 不能为空");
        }
        return value.trim();
    }

    private void ensureNoApplications(Long taskId) {
        long applicationCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, taskId)
                        .in(Application::getStatus, ApplicationStatus.PENDING, ApplicationStatus.APPROVED));
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
        vo.setAnonymous(task.getAnonymous());

        if (Boolean.TRUE.equals(task.getAnonymous())) {
            vo.setPublisherId(null);
            vo.setPublisherNickname("匿名用户");
            vo.setPublisherAvatarUrl(null);
        } else {
            vo.setPublisherId(task.getPublisherId());
            UserProfile publisherProfile = findProfile(task.getPublisherId());
            vo.setPublisherNickname(publisherProfile != null ? publisherProfile.getNickname() : "CampusHub 用户");
            vo.setPublisherAvatarUrl(publisherProfile != null ? publisherProfile.getAvatarUrl() : null);
        }

        vo.setCategory(task.getCategory());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setCampus(task.getCampus());
        vo.setRewardType(task.getRewardType());
        vo.setRewardAmount(task.getRewardAmount());
        vo.setPaymentMethod(task.getPaymentMethod());
        vo.setDeadline(task.getDeadline());
        vo.setStatus(task.getStatus());
        vo.setImageUrls(taskImageMapper.selectList(new LambdaQueryWrapper<TaskImage>()
                        .eq(TaskImage::getTaskId, task.getId())
                        .orderByAsc(TaskImage::getSortOrder))
                .stream()
                .map(TaskImage::getImageUrl)
                .toList());
        vo.setApplicationCount(applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, task.getId())
                        .in(Application::getStatus, ApplicationStatus.PENDING, ApplicationStatus.APPROVED)));
        if (Objects.equals(SecurityUtils.getCurrentUserId().orElse(null), task.getPublisherId())) {
            vo.setHasUnreadApplications(applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                    .eq(Application::getTaskId, task.getId())
                    .eq(Application::getPublisherViewed, false)) > 0);
        }
        vo.setFavoriteCount(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>().eq(Favorite::getTaskId, task.getId())));
        vo.setFavorited(isFavoritedByCurrentUser(task.getId()));
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        Map<String, Object> categoryFields = parseCategoryFields(task.getCategoryFields());
        Set<String> privateFieldNames = PRIVATE_CATEGORY_FIELDS.getOrDefault(task.getCategory(), Set.of());
        if (!privateFieldNames.isEmpty() && !canCurrentUserViewPrivateFields(task)) {
            Map<String, Object> publicFields = new LinkedHashMap<>(categoryFields);
            privateFieldNames.stream()
                    .map(categoryFields::get)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(value -> !value.isBlank())
                    .forEach(value -> {
                        vo.setTitle(redactPrivateValue(vo.getTitle(), value));
                        vo.setDescription(redactPrivateValue(vo.getDescription(), value));
                    });
            privateFieldNames.forEach(publicFields::remove);
            vo.setPrivateFieldsHidden(publicFields.size() != categoryFields.size());
            categoryFields = publicFields;
        }
        vo.setCategoryFields(categoryFields);
        return vo;
    }

    private boolean canCurrentUserViewPrivateFields(Task task) {
        Optional<Long> currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return false;
        }
        if (Objects.equals(currentUserId.get(), task.getPublisherId())) {
            return true;
        }
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getTaskId, task.getId())
                .eq(Order::getServiceProviderId, currentUserId.get())) > 0;
    }

    private String redactPrivateValue(String publicText, String privateValue) {
        if (publicText == null || !publicText.contains(privateValue)) {
            return publicText;
        }
        return publicText.replace(privateValue, "[私密信息已隐藏]");
    }

    private void applyReward(
            Task task,
            RewardType rewardType,
            BigDecimal rewardAmount,
            RewardPaymentMethod paymentMethod
    ) {
        if (RewardType.CASH.equals(rewardType)) {
            if (rewardAmount == null || rewardAmount.signum() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "金额必须大于 0");
            }
            if (paymentMethod == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择结算方式");
            }
            task.setRewardAmount(rewardAmount);
            task.setPaymentMethod(paymentMethod);
            return;
        }
        task.setRewardAmount(null);
        task.setPaymentMethod(null);
    }

    private boolean isFavoritedByCurrentUser(Long taskId) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTaskId, taskId)) > 0)
                .orElse(false);
    }

    private ApplicationItemVO toApplicationItemVO(Application application) {
        ApplicationItemVO vo = new ApplicationItemVO();
        vo.setId(application.getId());
        vo.setTaskId(application.getTaskId());
        vo.setApplicantId(application.getApplicantId());

        UserProfile profile = findProfile(application.getApplicantId());
        vo.setApplicantNickname(profile != null ? profile.getNickname() : "CampusHub 用户");
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

    private Task requireFreshTask(Long taskId) {
        Task task = requireTask(taskId);
        expireOpenTaskIfPastDeadline(task);
        return task;
    }

    private void expireOpenTasksPastDeadline() {
        taskMapper.expireOpenTasksPastDeadline();
        orderMapper.timeoutPendingConfirmOrdersForExpiredTasks();
        orderMapper.timeoutInProgressOrdersPastTaskDeadline();
        taskMapper.expireInProgressTasksWithTimedOutOrders();
    }

    private void expireOpenTaskIfPastDeadline(Task task) {
        if (task.getDeadline() == null || task.getDeadline().isAfter(LocalDateTime.now())) {
            return;
        }

        if (TaskStatus.OPEN.equals(task.getStatus())) {
            int updatedRows = taskMapper.updateStatusIfCurrent(
                    task.getId(),
                    TaskStatus.OPEN.name(),
                    TaskStatus.EXPIRED.name()
            );
            if (updatedRows == 1) {
                task.setStatus(TaskStatus.EXPIRED);
                orderMapper.timeoutPendingConfirmOrderByTaskId(task.getId());
            } else {
                refreshTaskStatus(task);
            }
            return;
        }

        if (TaskStatus.IN_PROGRESS.equals(task.getStatus())) {
            int orderUpdatedRows = orderMapper.timeoutInProgressOrderByTaskId(task.getId());
            if (orderUpdatedRows > 0) {
                int taskUpdatedRows = taskMapper.updateStatusIfCurrent(
                        task.getId(),
                        TaskStatus.IN_PROGRESS.name(),
                        TaskStatus.EXPIRED.name()
                );
                if (taskUpdatedRows == 1) {
                    task.setStatus(TaskStatus.EXPIRED);
                } else {
                    refreshTaskStatus(task);
                }
            }
        }
    }

    private void refreshTaskStatus(Task task) {
        Task refreshed = taskMapper.selectById(task.getId());
        if (refreshed != null) {
            task.setStatus(refreshed.getStatus());
        }
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
        return clampCreditScore(latest != null ? latest.getScoreAfter() : DEFAULT_CREDIT_SCORE);
    }

    private int clampCreditScore(int score) {
        return Math.max(MIN_CREDIT_SCORE, Math.min(MAX_CREDIT_SCORE, score));
    }

    private String toJson(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类扩展字段格式无效");
        }
    }

    private void bindTaskFiles(Long taskId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;
        int index = 0;
        for (Long fileId : fileIds) {
            FileRecord file = fileService.requireOwnedFile(fileId, UploadBusinessType.TASK_FILE);
            TaskFile taskFile = new TaskFile();
            taskFile.setTaskId(taskId);
            taskFile.setFileRecordId(file.getId());
            taskFile.setSortOrder(index++);
            taskFileMapper.insert(taskFile);
        }
    }

    public List<TaskFileVO> listTaskFiles(Long taskId) {
        return taskFileMapper.selectList(new LambdaQueryWrapper<TaskFile>()
                        .eq(TaskFile::getTaskId, taskId)
                        .orderByAsc(TaskFile::getSortOrder))
                .stream()
                .map(item -> {
                    FileRecord file = fileService.requireFile(item.getFileRecordId());
                    return new TaskFileVO(item.getId(), file.getFileName(), file.getFileSize());
                })
                .toList();
    }

    public FileRecord requireTaskFileForProvider(Long taskId, Long taskFileId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        TaskFile taskFile = taskFileMapper.selectOne(new LambdaQueryWrapper<TaskFile>()
                .eq(TaskFile::getId, taskFileId)
                .eq(TaskFile::getTaskId, taskId)
                .last("LIMIT 1"));
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getTaskId, taskId)
                .last("LIMIT 1"));
        if (taskFile == null || order == null || !Objects.equals(currentUserId, order.getServiceProviderId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有服务方可以下载任务文件");
        }
        return fileService.requireFile(taskFile.getFileRecordId());
    }

    private boolean canCurrentUserDownloadTaskFiles(Long taskId) {
        Optional<Long> currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.isEmpty()) return false;
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getTaskId, taskId)
                .last("LIMIT 1"));
        return order != null && Objects.equals(currentUserId.get(), order.getServiceProviderId());
    }

    private Map<String, Object> normalizeCategoryFields(TaskCategory category, Map<String, Object> fields) {
        if (!TaskCategory.TEAM_UP.equals(category)) {
            return fields;
        }

        Map<String, Object> normalized = fields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fields);
        Object contactInfo = normalized.get("contactInfo");
        if (contactInfo == null || contactInfo.toString().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "组队搭子帖子必须填写联系方式");
        }
        normalized.put("contactInfo", contactInfo.toString().trim());

        Object requiredCount = normalized.get("requiredCount");
        if (requiredCount == null || requiredCount.toString().isBlank()) {
            normalized.put("requiredCount", 1);
            return normalized;
        }

        try {
            int value = Integer.parseInt(requiredCount.toString());
            if (value < 1) {
                throw new NumberFormatException();
            }
            normalized.put("requiredCount", value);
            return normalized;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "人数需求必须是大于等于 1 的整数");
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "需求分类无效");
        }
    }
}
