package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.entity.AdminOperationLog;
import com.campushub.entity.Announcement;
import com.campushub.entity.CreditLog;
import com.campushub.entity.Order;
import com.campushub.entity.Report;
import com.campushub.entity.Task;
import com.campushub.entity.User;
import com.campushub.entity.UserProfile;
import com.campushub.dto.admin.AdminAnnouncementCreateRequest;
import com.campushub.dto.admin.AdminAnnouncementUpdateRequest;
import com.campushub.dto.admin.AdminOrderStatusUpdateRequest;
import com.campushub.dto.admin.AdminReportQueryRequest;
import com.campushub.dto.admin.AdminTaskStatusUpdateRequest;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.ReportStatus;
import com.campushub.enums.TaskStatus;
import com.campushub.enums.UserStatus;
import com.campushub.mapper.AdminOperationLogMapper;
import com.campushub.mapper.AnnouncementMapper;
import com.campushub.mapper.CreditLogMapper;
import com.campushub.mapper.OrderMapper;
import com.campushub.mapper.ReportMapper;
import com.campushub.mapper.TaskMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.UserProfileMapper;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.admin.AdminDashboardVO;
import com.campushub.vo.admin.AdminOrderItemVO;
import com.campushub.vo.admin.AdminReportItemVO;
import com.campushub.vo.admin.AdminTaskItemVO;
import com.campushub.vo.admin.AdminUserDetailVO;
import com.campushub.vo.admin.AdminUserItemVO;
import com.campushub.vo.admin.AdminUserStatusVO;
import com.campushub.vo.announcement.AnnouncementItemVO;
import com.campushub.vo.announcement.AnnouncementPublishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final CreditLogMapper creditLogMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final AnnouncementMapper announcementMapper;
    private final TaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final ReportMapper reportMapper;

    public AdminDashboardVO getDashboard() {
        long totalUsers = userMapper.selectCount(null);
        long activeTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, TaskStatus.OPEN));
        long inProgressOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, List.of(OrderStatus.IN_PROGRESS, OrderStatus.PENDING_COMPLETION)));
        long pendingReports = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .in(Report::getStatus, List.of(ReportStatus.PENDING, ReportStatus.PROCESSING)));
        return new AdminDashboardVO(totalUsers, activeTasks, inProgressOrders, pendingReports);
    }

    public PageResult<AdminUserItemVO> listUsers(int page, int size, String keyword, UserStatus status, Boolean verified) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String trimmedKeyword = keyword == null ? null : keyword.trim();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (verified != null) {
            wrapper.eq(User::getVerified, verified);
        }
        if (StringUtils.hasText(trimmedKeyword)) {
            List<Long> matchedUserIds = userProfileMapper.selectList(
                            new LambdaQueryWrapper<UserProfile>()
                                    .like(UserProfile::getNickname, trimmedKeyword)
                    )
                    .stream()
                    .map(UserProfile::getUserId)
                    .toList();
            wrapper.and(w -> {
                w.like(User::getEmail, trimmedKeyword);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in(User::getId, matchedUserIds);
                }
            });
        }

        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<Long> userIds = result.getRecords().stream().map(User::getId).toList();
        Map<Long, UserProfile> profilesByUserId = loadProfiles(userIds);

        List<AdminUserItemVO> records = result.getRecords().stream()
                .map(user -> toAdminUserItemVO(user, profilesByUserId.get(user.getId())))
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    public AdminUserDetailVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        return new AdminUserDetailVO(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getNickname() : "",
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getBio() : null,
                user.getRole(),
                user.getStatus(),
                user.getVerified(),
                findLatestCreditScore(user.getId()),
                user.getCreatedAt()
        );
    }

    @Transactional
    public AdminUserStatusVO updateUserStatus(Long userId, UserStatus status, String reason) {
        if (status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "User status is required");
        }
        if (UserStatus.ANONYMIZED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Admin user status update only supports ACTIVE or DISABLED");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        if (UserStatus.ACTIVE.equals(status)) {
            user.setLoginFailures(0);
            user.setLockedUntil(null);
        }
        userMapper.updateById(user);

        recordUserStatusOperation(userId, status, reason);
        return new AdminUserStatusVO(userId, status);
    }

    public PageResult<AnnouncementItemVO> listAnnouncements(int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        Page<Announcement> result = announcementMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getCreatedAt)
        );

        List<AnnouncementItemVO> records = result.getRecords().stream()
                .map(this::toAnnouncementItemVO)
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    public PageResult<AdminTaskItemVO> listTasks(int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        Page<Task> result = taskMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Task>()
                        .orderByDesc(Task::getCreatedAt)
        );

        List<AdminTaskItemVO> records = result.getRecords().stream()
                .map(task -> new AdminTaskItemVO(
                        task.getId(),
                        task.getTitle(),
                        task.getCategory(),
                        task.getPublisherId(),
                        task.getStatus(),
                        task.getDeadline(),
                        task.getCreatedAt()
                ))
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Transactional
    public void updateTaskStatus(Long taskId, AdminTaskStatusUpdateRequest request) {
        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Task status is required");
        }
        Task task = requireTask(taskId);
        if (!TaskStatus.OPEN.equals(request.getStatus()) && !TaskStatus.CANCELLED.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Admin task status update only supports OPEN or CANCELLED");
        }
        if (TaskStatus.OPEN.equals(task.getStatus()) && !TaskStatus.CANCELLED.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Open tasks can only be taken down to CANCELLED by admin");
        }
        if (TaskStatus.CANCELLED.equals(task.getStatus()) && !TaskStatus.OPEN.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Cancelled tasks can only be restored to OPEN by admin");
        }
        if (!TaskStatus.OPEN.equals(task.getStatus()) && !TaskStatus.CANCELLED.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only OPEN or CANCELLED tasks support admin status adjustment");
        }
        TaskStatus nextStatus = request.getStatus();
        if (TaskStatus.OPEN.equals(task.getStatus()) && TaskStatus.CANCELLED.equals(nextStatus)) {
            task.setStatus(TaskStatus.CANCELLED);
            taskMapper.updateById(task);
            recordAdminOperation("UPDATE_TASK_STATUS", "TASK", taskId,
                    buildAdminTransitionDetail(TaskStatus.OPEN.name(), TaskStatus.CANCELLED.name(), request.getReason()));
            return;
        }
        String previousStatus = findPreviousAdminStatus("UPDATE_TASK_STATUS", "TASK", taskId);
        if (!TaskStatus.OPEN.name().equals(previousStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only admin-taken-down tasks can be restored to OPEN");
        }
        task.setStatus(TaskStatus.OPEN);
        taskMapper.updateById(task);
        recordAdminOperation("UPDATE_TASK_STATUS", "TASK", taskId,
                buildAdminTransitionDetail(TaskStatus.CANCELLED.name(), TaskStatus.OPEN.name(), request.getReason()));
    }

    public PageResult<AdminOrderItemVO> listOrders(int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        Page<Order> result = orderMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Order>()
                        .orderByDesc(Order::getCreatedAt)
        );

        List<Long> taskIds = result.getRecords().stream().map(Order::getTaskId).distinct().toList();
        Map<Long, String> taskTitles = taskIds.isEmpty()
                ? Map.of()
                : taskMapper.selectList(new LambdaQueryWrapper<Task>().in(Task::getId, taskIds))
                .stream()
                .collect(Collectors.toMap(Task::getId, Task::getTitle, (left, right) -> left));

        List<AdminOrderItemVO> records = result.getRecords().stream()
                .map(order -> new AdminOrderItemVO(
                        order.getId(),
                        order.getTaskId(),
                        taskTitles.getOrDefault(order.getTaskId(), ""),
                        order.getPublisherId(),
                        order.getServiceProviderId(),
                        order.getStatus(),
                        order.getCreatedAt()
                ))
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, AdminOrderStatusUpdateRequest request) {
        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Order status is required");
        }
        Order order = requireOrder(orderId);
        OrderStatus requestedStatus = request.getStatus();
        if (OrderStatus.DISPUTE.equals(requestedStatus)) {
            if (OrderStatus.DISPUTE.equals(order.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Order is already disputed");
            }
            if (OrderStatus.COMPLETED.equals(order.getStatus())
                    || OrderStatus.CANCELLED.equals(order.getStatus())
                    || OrderStatus.REVIEWED.equals(order.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Completed, cancelled or reviewed orders cannot be frozen");
            }
        } else {
            if (!OrderStatus.DISPUTE.equals(order.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Only disputed orders can be restored by admin");
            }
        }
        OrderStatus currentStatus = order.getStatus();
        if (OrderStatus.DISPUTE.equals(requestedStatus)) {
            order.setStatus(OrderStatus.DISPUTE);
            orderMapper.updateById(order);
            recordAdminOperation("UPDATE_ORDER_STATUS", "ORDER", orderId,
                    buildAdminTransitionDetail(currentStatus.name(), OrderStatus.DISPUTE.name(), request.getReason()));
            return;
        }
        String previousStatus = findPreviousAdminStatus("UPDATE_ORDER_STATUS", "ORDER", orderId);
        OrderStatus restoreStatus;
        try {
            restoreStatus = OrderStatus.valueOf(previousStatus);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Missing original order status for admin restore");
        }
        if (OrderStatus.DISPUTE.equals(restoreStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Cannot restore order to disputed status");
        }
        if (!requestedStatus.equals(restoreStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Restore target must match the original order status before dispute: " + restoreStatus.name());
        }
        order.setStatus(restoreStatus);
        orderMapper.updateById(order);
        recordAdminOperation("UPDATE_ORDER_STATUS", "ORDER", orderId,
                buildAdminTransitionDetail(currentStatus.name(), restoreStatus.name(), request.getReason()));
    }

    public PageResult<AdminReportItemVO> listReports(int page, int size, AdminReportQueryRequest request) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getCreatedAt);

        if (request != null) {
            if (request.getStatus() != null) {
                wrapper.eq(Report::getStatus, request.getStatus());
            }
            if (request.getTargetType() != null) {
                wrapper.eq(Report::getTargetType, request.getTargetType());
            }
            if (StringUtils.hasText(request.getKeyword())) {
                wrapper.like(Report::getDescription, request.getKeyword().trim());
            }
        }

        Page<Report> result = reportMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<AdminReportItemVO> records = result.getRecords().stream()
                .map(report -> new AdminReportItemVO(
                        report.getId(),
                        report.getReporterId(),
                        report.getTargetType(),
                        report.getTargetId(),
                        report.getDescription(),
                        report.getStatus(),
                        report.getCreatedAt()
                ))
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    public PageResult<AnnouncementItemVO> listPublicAnnouncements(int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        Page<Announcement> result = announcementMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getIsActive, true)
                        .orderByDesc(Announcement::getCreatedAt)
        );

        List<AnnouncementItemVO> records = result.getRecords().stream()
                .map(this::toAnnouncementItemVO)
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Transactional
    public AnnouncementPublishVO createAnnouncement(AdminAnnouncementCreateRequest request) {
        Announcement announcement = new Announcement();
        announcement.setPublisherId(SecurityUtils.requireCurrentUserId());
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setPriority(StringUtils.hasText(request.getPriority()) ? request.getPriority().trim() : "NORMAL");
        announcement.setIsActive(true);
        announcementMapper.insert(announcement);
        return new AnnouncementPublishVO(announcement.getId(), "PUBLISHED");
    }

    @Transactional
    public AnnouncementPublishVO updateAnnouncement(Long announcementId, AdminAnnouncementUpdateRequest request) {
        Announcement announcement = requireAnnouncement(announcementId);

        if (request.getTitle() != null) {
            announcement.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            announcement.setContent(request.getContent().trim());
        }
        if (request.getPriority() != null) {
            announcement.setPriority(request.getPriority().trim());
        }
        if (request.getIsActive() != null) {
            announcement.setIsActive(request.getIsActive());
        }

        announcementMapper.updateById(announcement);
        return new AnnouncementPublishVO(
                announcement.getId(),
                Boolean.TRUE.equals(announcement.getIsActive()) ? "ACTIVE" : "INACTIVE"
        );
    }

    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        requireAnnouncement(announcementId);
        announcementMapper.deleteById(announcementId);
    }

    private int normalizePage(int page) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Size must be greater than or equal to 1");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, UserProfile> loadProfiles(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileMapper.selectList(
                        new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds)
                )
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (left, right) -> left));
    }

    private AdminUserItemVO toAdminUserItemVO(User user, UserProfile profile) {
        return new AdminUserItemVO(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getNickname() : "",
                user.getRole(),
                user.getStatus(),
                user.getVerified(),
                findLatestCreditScore(user.getId()),
                user.getCreatedAt()
        );
    }

    private Integer findLatestCreditScore(Long userId) {
        CreditLog latest = creditLogMapper.selectOne(new LambdaQueryWrapper<CreditLog>()
                .eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getId)
                .last("LIMIT 1"));
        return latest != null ? latest.getScoreAfter() : 100;
    }

    private void recordUserStatusOperation(Long userId, UserStatus status, String reason) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(SecurityUtils.requireCurrentUserId());
        log.setOperationType(UserStatus.DISABLED.equals(status) ? "DISABLE_USER" : "ENABLE_USER");
        log.setTargetType("USER");
        log.setTargetId(userId);
        log.setDetail(buildStatusOperationDetail(status, reason));
        adminOperationLogMapper.insert(log);
    }

    private String buildStatusOperationDetail(UserStatus status, String reason) {
        String detail = "Set user status to " + status.getValue();
        if (StringUtils.hasText(reason)) {
            detail += ". Reason: " + reason.trim();
        }
        return detail;
    }

    private String buildAdminActionDetail(String action, String reason) {
        String detail = "Set status to " + action;
        if (StringUtils.hasText(reason)) {
            detail += ". Reason: " + reason.trim();
        }
        return detail;
    }

    private String buildAdminTransitionDetail(String fromStatus, String toStatus, String reason) {
        String detail = "Transition " + fromStatus + " -> " + toStatus;
        if (StringUtils.hasText(reason)) {
            detail += ". Reason: " + reason.trim();
        }
        return detail;
    }

    private String findPreviousAdminStatus(String operationType, String targetType, Long targetId) {
        AdminOperationLog latest = adminOperationLogMapper.selectOne(
                new LambdaQueryWrapper<AdminOperationLog>()
                        .eq(AdminOperationLog::getOperationType, operationType)
                        .eq(AdminOperationLog::getTargetType, targetType)
                        .eq(AdminOperationLog::getTargetId, targetId)
                        .orderByDesc(AdminOperationLog::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (latest == null || !StringUtils.hasText(latest.getDetail())) {
            return null;
        }
        String detail = latest.getDetail().trim();
        String prefix = "Transition ";
        int arrowIndex = detail.indexOf(" -> ");
        if (!detail.startsWith(prefix) || arrowIndex <= prefix.length()) {
            return null;
        }
        return detail.substring(prefix.length(), arrowIndex).trim();
    }

    private void recordAdminOperation(String operationType, String targetType, Long targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(SecurityUtils.requireCurrentUserId());
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        adminOperationLogMapper.insert(log);
    }

    private Announcement requireAnnouncement(Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Announcement not found");
        }
        return announcement;
    }

    private Task requireTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private AnnouncementItemVO toAnnouncementItemVO(Announcement announcement) {
        return new AnnouncementItemVO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getPriority(),
                announcement.getIsActive(),
                announcement.getPublisherId(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
