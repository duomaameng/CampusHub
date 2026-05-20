package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.request.CreateTaskRequest;
import com.campushub.dto.response.TaskDetailResponse;
import com.campushub.dto.response.TaskListItem;
import com.campushub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ApiResponse<PageResult<TaskListItem>> listTasks(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(taskService.listTasks(category, campus, keyword, sort, status, page, size));
    }

    @PostMapping
    public ApiResponse<TaskDetailResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskDetailResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getTask(taskId));
    }

    @PatchMapping("/{taskId}")
    public ApiResponse<TaskDetailResponse> updateTask(@PathVariable Long taskId,
                                                       @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.updateTask(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.success();
    }

    @PostMapping("/{taskId}/favorite")
    public ApiResponse<Void> toggleFavorite(@PathVariable Long taskId) {
        taskService.toggleFavorite(taskId);
        return ApiResponse.success();
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<TaskListItem>> getFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(taskService.getFavorites(page, size));
    }
}
