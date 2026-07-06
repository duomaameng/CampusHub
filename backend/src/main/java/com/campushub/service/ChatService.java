package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.message.ChatMessageRequest;
import com.campushub.entity.ChatMessage;
import com.campushub.entity.Conversation;
import com.campushub.entity.FileRecord;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.enums.MessageType;
import com.campushub.enums.UploadBusinessType;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.ChatMessageMapper;
import com.campushub.mapper.ConversationMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.realtime.RealtimeEventPublisher;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.message.ChatDetailVO;
import com.campushub.vo.message.ChatMessageVO;
import com.campushub.vo.message.ChatUserVO;
import com.campushub.vo.message.ConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final FileService fileService;
    private final NotificationService notificationService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public List<ConversationVO> listConversations() {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                        .and(w -> w.eq(Conversation::getUser1Id, currentUserId)
                                .or()
                                .eq(Conversation::getUser2Id, currentUserId))
                        .orderByDesc(Conversation::getUpdatedAt))
                .stream()
                .map(conversation -> toConversationVO(conversation, currentUserId))
                .toList();
    }

    public ChatDetailVO getChat(Long participantId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ChatUserVO participant = requireChatUser(participantId, currentUserId);
        Conversation conversation = findConversation(currentUserId, participantId);
        List<ChatMessageVO> messages = conversation == null
                ? List.of()
                : chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversation.getId())
                        .orderByAsc(ChatMessage::getCreatedAt))
                        .stream()
                        .map(this::toMessageVO)
                        .toList();
        return ChatDetailVO.builder()
                .conversationId(conversation == null ? null : conversation.getId())
                .participant(participant)
                .messages(messages)
                .build();
    }

    @Transactional
    public ChatMessageVO sendMessage(Long participantId, ChatMessageRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ChatUserVO participant = requireChatUser(participantId, currentUserId);
        Conversation conversation = requireConversation(currentUserId, participantId);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(currentUserId);
        message.setMessageType(request.getMessageType());
        message.setIsRead(false);

        if (MessageType.TEXT.equals(request.getMessageType())) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.MESSAGE_EMPTY);
            }
            message.setContent(request.getContent().trim());
        } else if (MessageType.IMAGE.equals(request.getMessageType())) {
            FileRecord image = request.getImageId() == null
                    ? null
                    : fileService.requireOwnedFile(request.getImageId(), UploadBusinessType.CHAT_IMAGE);
            if (image == null) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "图片文件不存在");
            }
            message.setImageUrl(image.getFileUrl());
            message.setFileId(image.getId());
        } else if (MessageType.FILE.equals(request.getMessageType())) {
            FileRecord file = request.getFileId() == null
                    ? null
                    : fileService.requireOwnedFile(request.getFileId(), UploadBusinessType.CHAT_FILE);
            if (file == null) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "附件不存在");
            }
            message.setFileId(file.getId());
        }

        chatMessageMapper.insert(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        String preview = switch (request.getMessageType()) {
            case IMAGE -> "发送了一张图片";
            case FILE -> "发送了一个文件";
            default -> message.getContent();
        };
        notificationService.createChatMessageNotification(
                participantId,
                currentUserId,
                findProfile(currentUserId).getNickname(),
                preview
        );
        publishMessageChange(currentUserId, participantId);
        return toMessageVO(message);
    }

    public long countUnreadMessages() {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        List<Long> conversationIds = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                        .and(w -> w.eq(Conversation::getUser1Id, currentUserId)
                                .or()
                                .eq(Conversation::getUser2Id, currentUserId)))
                .stream()
                .map(Conversation::getId)
                .toList();
        if (conversationIds.isEmpty()) {
            return 0;
        }
        return chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .in(ChatMessage::getConversationId, conversationIds)
                .ne(ChatMessage::getSenderId, currentUserId)
                .eq(ChatMessage::getIsRead, false));
    }

    @Transactional
    public void markMessagesRead(Long participantId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        requireChatUser(participantId, currentUserId);
        Conversation conversation = findConversation(currentUserId, participantId);
        if (conversation == null) {
            return;
        }
        int updated = chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .ne(ChatMessage::getSenderId, currentUserId)
                .eq(ChatMessage::getIsRead, false)
                .set(ChatMessage::getIsRead, true));
        if (updated > 0) {
            realtimeEventPublisher.user(currentUserId, RealtimeEventPublisher.MESSAGES_CHANGED, participantId);
        }
    }

    public FileRecord requireMessageAttachment(Long messageId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || message.getFileId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息附件不存在");
        }
        Conversation conversation = conversationMapper.selectById(message.getConversationId());
        ensureParticipant(conversation, currentUserId);
        return fileService.requireFile(message.getFileId());
    }

    private Conversation requireConversation(Long currentUserId, Long participantId) {
        Conversation existing = findConversation(currentUserId, participantId);
        if (existing != null) {
            return existing;
        }
        Conversation conversation = new Conversation();
        conversation.setUser1Id(Math.min(currentUserId, participantId));
        conversation.setUser2Id(Math.max(currentUserId, participantId));
        try {
            conversationMapper.insert(conversation);
            return conversation;
        } catch (DuplicateKeyException ignored) {
            return findConversation(currentUserId, participantId);
        }
    }

    private Conversation findConversation(Long firstUserId, Long secondUserId) {
        long user1Id = Math.min(firstUserId, secondUserId);
        long user2Id = Math.max(firstUserId, secondUserId);
        return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, user1Id)
                .eq(Conversation::getUser2Id, user2Id)
                .last("LIMIT 1"));
    }

    private ConversationVO toConversationVO(Conversation conversation, Long currentUserId) {
        Long participantId = Objects.equals(conversation.getUser1Id(), currentUserId)
                ? conversation.getUser2Id()
                : conversation.getUser1Id();
        ChatMessage latest = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT 1"));
        long unreadCount = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .ne(ChatMessage::getSenderId, currentUserId)
                .eq(ChatMessage::getIsRead, false));
        return ConversationVO.builder()
                .id(conversation.getId())
                .participant(requireChatUser(participantId, currentUserId))
                .lastMessageText(messagePreview(latest))
                .lastMessageAt(latest == null ? conversation.getUpdatedAt() : latest.getCreatedAt())
                .unreadCount(unreadCount)
                .build();
    }

    private String messagePreview(ChatMessage message) {
        if (message == null) return "还没有消息";
        return switch (message.getMessageType()) {
            case IMAGE -> "[图片]";
            case FILE -> "[文件] " + fileService.requireFile(message.getFileId()).getFileName();
            default -> message.getContent();
        };
    }

    private ChatMessageVO toMessageVO(ChatMessage message) {
        UserProfile sender = findProfile(message.getSenderId());
        FileRecord file = message.getFileId() == null ? null : fileService.requireFile(message.getFileId());
        return ChatMessageVO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderNickname(sender.getNickname())
                .senderAvatarUrl(sender.getAvatarUrl())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .fileId(message.getFileId())
                .fileName(file == null ? null : file.getFileName())
                .fileSize(file == null ? null : file.getFileSize())
                .read(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ChatUserVO requireChatUser(Long userId, Long currentUserId) {
        if (Objects.equals(userId, currentUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能与自己聊天");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfile profile = findProfile(userId);
        return ChatUserVO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(profile.getNickname())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }

    private UserProfile findProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return profile;
    }

    private void ensureParticipant(Conversation conversation, Long currentUserId) {
        if (conversation == null || (!Objects.equals(conversation.getUser1Id(), currentUserId)
                && !Objects.equals(conversation.getUser2Id(), currentUserId))) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_PARTICIPANT);
        }
    }

    private void publishMessageChange(Long senderId, Long receiverId) {
        realtimeEventPublisher.user(senderId, RealtimeEventPublisher.MESSAGES_CHANGED, receiverId);
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.MESSAGES_CHANGED, senderId);
    }
}
