package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.dto.request.CreateTaskRequest;
import com.campushub.dto.response.TaskDetailResponse;
import com.campushub.dto.response.TaskListItem;
import com.campushub.entity.*;
import com.campushub.enums.TaskStatus;
import com.campushub.mapper.*;
import com.campushub.security.SecurityUtils;
import com.campushub.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskImageMapper taskImageMapper;
    private final FavoriteMapper favoriteMapper;
    private final ApplicationMapper applicationMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public PageResult<TaskListItem> listTasks(String category, String campus, String keyword,
                                              String sort, String status, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, status != null ? TaskStatus.valueOf(status) : TaskStatus.OPEN);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Task::getCategory, category);
        }
        if (campus != null && !campus.isEmpty()) {
            wrapper.eq(Task::getCampus, campus);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Task::getTitle, keyword).or().like(Task::getDescription, keyword));
        }
        if ("deadline".equals(sort)) {
            wrapper.orderByAsc(Task::getDeadline);
        } else {
            wrapper.orderByDesc(Task::getCreatedAt);
        }

        IPage<Task> taskPage = taskMapper.selectPage(new Page<>(page, size), wrapper);
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        List<TaskListItem> records = taskPage.getRecords().stream()
                .map(task -> toListItem(task, currentUserId))
                .collect(Collectors.toList());

        return PageResult.of(taskPage.getTotal(), page, size, records);
    }

    @Override
    public TaskDetailResponse getTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return toDetail(task, currentUserId);
    }

    @Override
    @Transactional
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();

        Task task = new Task();
        task.setPublisherId(userId);
        task.setCategory(request.getCategory());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCampus(request.getCampus());
        task.setLocationDetail(request.getLocationDetail());
        task.setRewardType(request.getRewardType());
        task.setDeadline(request.getDeadline());
        task.setStatus(TaskStatus.OPEN);
        task.setAnonymous(request.getAnonymous() != null && request.getAnonymous());
        task.setCategoryFields(request.getCategoryFields());
        task.setVersion(0);
        taskMapper.insert(task);

        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            for (int i = 0; i < request.getImageIds().size(); i++) {
                TaskImage image = new TaskImage();
                image.setTaskId(task.getId());
                image.setImageUrl("/uploads/tasks/" + request.getImageIds().get(i));
                image.setSortOrder(i);
                taskImageMapper.insert(image);
            }
        }

        return toDetail(task, userId);
    }

    @Override
    @Transactional
    public TaskDetailResponse updateTask(Long taskId, CreateTaskRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }

        task.setCategory(request.getCategory());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCampus(request.getCampus());
        task.setLocationDetail(request.getLocationDetail());
        task.setRewardType(request.getRewardType());
        task.setDeadline(request.getDeadline());
        task.setAnonymous(request.getAnonymous() != null && request.getAnonymous());
        task.setCategoryFields(request.getCategoryFields());
        taskMapper.updateById(task);

        return toDetail(task, userId);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_OWNER);
        }
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }

        taskMapper.deleteById(taskId);
    }

    @Override
    @Transactional
    public void toggleFavorite(Long taskId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTaskId, taskId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTaskId(taskId);
            favoriteMapper.insert(favorite);
        }
    }

    @Override
    public PageResult<TaskListItem> getFavorites(int page, int size) {
        Long userId = SecurityUtils.requireCurrentUserId();

        LambdaQueryWrapper<Favorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreatedAt);
        IPage<Favorite> favPage = favoriteMapper.selectPage(new Page<>(page, size), favWrapper);

        if (favPage.getRecords().isEmpty()) {
            return PageResult.of(0, page, size, Collections.emptyList());
        }

        Set<Long> taskIds = favPage.getRecords().stream()
                .map(Favorite::getTaskId).collect(Collectors.toSet());
        List<Task> tasks = taskMapper.selectBatchIds(taskIds);

        List<TaskListItem> records = tasks.stream()
                .map(task -> {
                    TaskListItem item = toListItem(task, userId);
                    item.setIsFavorited(true);
                    return item;
                })
                .collect(Collectors.toList());

        return PageResult.of(favPage.getTotal(), page, size, records);
    }

    private TaskListItem toListItem(Task task, Long currentUserId) {
        List<String> imageUrls = getImageUrls(task.getId());
        Long appCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getTaskId, task.getId())
                        .eq(Application::getStatus, "PENDING"));
        Long favCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getTaskId, task.getId()));
        boolean isFavorited = currentUserId != null && favoriteMapper.exists(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, currentUserId)
                        .eq(Favorite::getTaskId, task.getId()));

        String publisherNickname = getPublisherNickname(task);

        return TaskListItem.builder()
                .id(task.getId())
                .publisherId(Boolean.TRUE.equals(task.getAnonymous()) ? null : task.getPublisherId())
                .publisherNickname(Boolean.TRUE.equals(task.getAnonymous()) ? "匿名用户" : publisherNickname)
                .category(task.getCategory())
                .title(task.getTitle())
                .description(task.getDescription())
                .campus(task.getCampus())
                .rewardType(task.getRewardType())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .anonymous(Boolean.TRUE.equals(task.getAnonymous()))
                .imageUrls(imageUrls)
                .applicationCount(appCount.intValue())
                .favoriteCount(favCount.intValue())
                .isFavorited(isFavorited)
                .createdAt(task.getCreatedAt())
                .build();
    }

    private TaskDetailResponse toDetail(Task task, Long currentUserId) {
        List<String> imageUrls = getImageUrls(task.getId());
        Long appCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>().eq(Application::getTaskId, task.getId()));
        Long favCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getTaskId, task.getId()));
        boolean isFavorited = currentUserId != null && favoriteMapper.exists(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, currentUserId)
                        .eq(Favorite::getTaskId, task.getId()));

        String publisherNickname = getPublisherNickname(task);

        return TaskDetailResponse.builder()
                .id(task.getId())
                .publisherId(Boolean.TRUE.equals(task.getAnonymous()) ? null : task.getPublisherId())
                .publisherNickname(Boolean.TRUE.equals(task.getAnonymous()) ? "匿名用户" : publisherNickname)
                .category(task.getCategory())
                .title(task.getTitle())
                .description(task.getDescription())
                .campus(task.getCampus())
                .locationDetail(task.getLocationDetail())
                .rewardType(task.getRewardType())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .anonymous(Boolean.TRUE.equals(task.getAnonymous()))
                .imageUrls(imageUrls)
                .categoryFields(task.getCategoryFields())
                .applicationCount(appCount.intValue())
                .favoriteCount(favCount.intValue())
                .isFavorited(isFavorited)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private List<String> getImageUrls(Long taskId) {
        return taskImageMapper.selectList(
                        new LambdaQueryWrapper<TaskImage>()
                                .eq(TaskImage::getTaskId, taskId)
                                .orderByAsc(TaskImage::getSortOrder))
                .stream()
                .map(TaskImage::getImageUrl)
                .collect(Collectors.toList());
    }

    private String getPublisherNickname(Task task) {
        if (Boolean.TRUE.equals(task.getAnonymous())) return "匿名用户";
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, task.getPublisherId()));
        return profile != null ? profile.getNickname() : "未知用户";
    }
}
