package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.message.ChatMessageRequest;
import com.campushub.entity.FileRecord;
import com.campushub.service.ChatService;
import com.campushub.service.FileService;
import com.campushub.vo.UnreadCountVO;
import com.campushub.vo.message.ChatDetailVO;
import com.campushub.vo.message.ChatMessageVO;
import com.campushub.vo.message.ConversationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final FileService fileService;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> conversations() {
        return ApiResponse.success(chatService.listConversations());
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<ChatDetailVO> chat(@PathVariable Long userId) {
        return ApiResponse.success(chatService.getChat(userId));
    }

    @PostMapping("/users/{userId}")
    public ApiResponse<ChatMessageVO> sendMessage(
            @PathVariable Long userId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(chatService.sendMessage(userId, request));
    }

    @PatchMapping("/users/{userId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long userId) {
        chatService.markMessagesRead(userId);
        return ApiResponse.success();
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount() {
        return ApiResponse.success(new UnreadCountVO(chatService.countUnreadMessages()));
    }

    @GetMapping("/{messageId}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long messageId) {
        FileRecord file = chatService.requireMessageAttachment(messageId);
        Path path = fileService.resolveStoredFile(file);
        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (Exception ignored) {
            contentType = null;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                        : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new FileSystemResource(path));
    }
}
