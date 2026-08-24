package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.task.TaskCreateRequest;
import com.campushub.dto.task.TaskUpdateRequest;
import com.campushub.service.TaskService;
import com.campushub.service.FileService;
import com.campushub.entity.FileRecord;
import com.campushub.vo.task.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final FileService fileService;

    @GetMapping("/tasks")
    public ApiResponse<PageResult<TaskItemVO>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String campus,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String sort) {
        return ApiResponse.success(taskService.listTasks(page, size, category, campus, keyword, sort));
    }

    @GetMapping("/tasks/mine")
    public ApiResponse<PageResult<TaskItemVO>> mine(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "100") int size,
                                                    @RequestParam(required = false) String keyword) {
        return ApiResponse.success(taskService.listMyPublishedTasks(page, size, keyword));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskItemVO> get(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getTask(taskId));
    }

    @GetMapping("/tasks/{taskId}/files/{taskFileId}/download")
    public ResponseEntity<Resource> downloadTaskFile(@PathVariable Long taskId, @PathVariable Long taskFileId) {
        FileRecord file = taskService.requireTaskFileForProvider(taskId, taskFileId);
        Path path = fileService.resolveStoredFile(file);
        String contentType;
        try { contentType = Files.probeContentType(path); } catch (Exception ignored) { contentType = null; }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }

    @PostMapping("/tasks")
    public ApiResponse<TaskCreateVO> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    @PostMapping("/tasks/{taskId}/applications")
    public ApiResponse<TaskApplyVO> apply(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.applyTask(taskId));
    }

    @GetMapping("/tasks/{taskId}/applications")
    public ApiResponse<List<ApplicationItemVO>> applications(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.listApplications(taskId));
    }

    @PostMapping("/tasks/{taskId}/applications/viewed")
    public ApiResponse<Void> markApplicationsViewed(@PathVariable Long taskId) {
        taskService.markApplicationsViewed(taskId);
        return ApiResponse.success();
    }

    @PatchMapping("/tasks/{taskId}")
    public ApiResponse<TaskItemVO> update(@PathVariable Long taskId, @Valid @RequestBody TaskUpdateRequest request) {
        return ApiResponse.success(taskService.updateTask(taskId, request));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> delete(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.success();
    }

    @PostMapping("/tasks/{taskId}/favorite")
    public ApiResponse<FavoriteToggleVO> toggleFavorite(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.toggleFavorite(taskId));
    }

    @GetMapping("/tasks/favorites")
    public ApiResponse<PageResult<TaskItemVO>> favorites(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(taskService.getFavorites(page, size));
    }

    @PostMapping("/applications/{applicationId}/confirm")
    public ApiResponse<ApplicationConfirmVO> confirm(@PathVariable Long applicationId) {
        return ApiResponse.success(taskService.confirmApplication(applicationId));
    }

    @PostMapping("/applications/{applicationId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long applicationId) {
        taskService.rejectApplication(applicationId);
        return ApiResponse.success();
    }
}
