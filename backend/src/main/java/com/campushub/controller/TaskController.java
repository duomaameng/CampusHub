package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.task.TaskApplyRequest;
import com.campushub.dto.task.TaskCreateRequest;
import com.campushub.service.TaskService;
import com.campushub.vo.task.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/tasks")
    public ApiResponse<PageResult<TaskItemVO>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String campus,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String sort) {
        return ApiResponse.success(taskService.listTasks(page, size, category, campus, keyword, sort));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskItemVO> get(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getTask(taskId));
    }

    @PostMapping("/tasks")
    public ApiResponse<TaskCreateVO> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    @PostMapping("/tasks/{taskId}/applications")
    public ApiResponse<TaskApplyVO> apply(@PathVariable Long taskId, @Valid @RequestBody TaskApplyRequest request) {
        return ApiResponse.success(taskService.applyTask(taskId, request));
    }

    @GetMapping("/tasks/{taskId}/applications")
    public ApiResponse<List<ApplicationItemVO>> applications(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.listApplications(taskId));
    }

    @PostMapping("/applications/{applicationId}/confirm")
    public ApiResponse<ApplicationConfirmVO> confirm(@PathVariable Long applicationId) {
        return ApiResponse.success(taskService.confirmApplication(applicationId));
    }
}
