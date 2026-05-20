package com.campushub.service;

import com.campushub.common.PageResult;
import com.campushub.dto.request.CreateTaskRequest;
import com.campushub.dto.response.TaskDetailResponse;
import com.campushub.dto.response.TaskListItem;

public interface TaskService {

    PageResult<TaskListItem> listTasks(String category, String campus, String keyword,
                                       String sort, String status, int page, int size);

    TaskDetailResponse getTask(Long taskId);

    TaskDetailResponse createTask(CreateTaskRequest request);

    TaskDetailResponse updateTask(Long taskId, CreateTaskRequest request);

    void deleteTask(Long taskId);

    void toggleFavorite(Long taskId);

    PageResult<TaskListItem> getFavorites(int page, int size);
}
