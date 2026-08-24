package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.PageResult;
import com.campushub.dto.order.OrderCancelRequest;
import com.campushub.dto.order.OrderCompleteRequest;
import com.campushub.dto.order.ReviewCreateRequest;
import com.campushub.entity.*;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.TaskStatus;
import com.campushub.mapper.*;
import com.campushub.realtime.RealtimeEventPublisher;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.order.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderMapper orderMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private OrderStatusLogMapper orderStatusLogMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private CreditLogMapper creditLogMapper;
    @Mock private TaskImageMapper taskImageMapper;
    @Mock private TaskFileMapper taskFileMapper;
    @Mock private NotificationService notificationService;
    @Mock private FileService fileService;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private static final Long CURRENT_USER_ID = 10001L;
    private static final Long OTHER_USER_ID = 10002L;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(CURRENT_USER_ID);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId)
                .thenReturn(java.util.Optional.of(CURRENT_USER_ID));
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    private Order createOrder(Long id, Long taskId, Long publisherId, Long serviceProviderId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setTaskId(taskId);
        order.setPublisherId(publisherId);
        order.setServiceProviderId(serviceProviderId);
        order.setStatus(status);
        order.setVersion(0);
        order.setCreatedAt(LocalDateTime.now().minusDays(1));
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    private Task createTask(Long id, Long publisherId, String title) {
        Task task = new Task();
        task.setId(id);
        task.setPublisherId(publisherId);
        task.setTitle(title);
        task.setDescription("Test task");
        task.setCampus("Xianlin");
        task.setCreatedAt(LocalDateTime.now().minusDays(2));
        return task;
    }

    // ==================== getOrder ====================
    @Nested
    class GetOrderTests {

        @Test
        void shouldReturnOrderDetailWhenParticipant() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(taskMapper.selectById(10L)).thenReturn(createTask(10L, CURRENT_USER_ID, "Test"));
            when(orderStatusLogMapper.selectList(any())).thenReturn(Collections.emptyList());

            OrderDetailVO result = orderService.getOrder(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
            verify(orderMapper).selectById(1L);
        }

        @Test
        void shouldThrowWhenNotParticipant() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, 99999L, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.getOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非订单参与方");
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrder(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }
    }

    // ==================== completeOrder ====================
    @Nested
    class CompleteOrderTests {

        @Test
        void shouldCompleteOrderWhenServiceProviderInProgress() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            FileRecord proofFile = new FileRecord();
            proofFile.setId(100L);
            proofFile.setFileUrl("/uploads/proofs/test.jpg");
            when(fileService.requireOwnedFile(eq(100L), any())).thenReturn(proofFile);

            OrderCompleteRequest request = new OrderCompleteRequest();
            request.setProofImageId(100L);
            request.setNote("Done");

            orderService.completeOrder(1L, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_COMPLETION);
            verify(orderMapper).updateById(order);
            verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
            verify(notificationService).createOrderStatusNotification(
                    eq(OTHER_USER_ID), eq(1L), eq(OrderStatus.PENDING_COMPLETION));
        }

        @Test
        void shouldThrowWhenNotServiceProvider() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCompleteRequest request = new OrderCompleteRequest();

            assertThatThrownBy(() -> orderService.completeOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非订单参与方");
        }

        @Test
        void shouldThrowWhenNotInProgressStatus() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.PENDING_COMPLETION);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCompleteRequest request = new OrderCompleteRequest();

            assertThatThrownBy(() -> orderService.completeOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单状态不允许");
        }

        @Test
        void shouldThrowWhenPendingCancelRequest() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.IN_PROGRESS);
            order.setCancelReason("Cannot fulfill");
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCompleteRequest request = new OrderCompleteRequest();

            assertThatThrownBy(() -> orderService.completeOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单有未处理的取消申请");
        }

        @Test
        void shouldSetProofUrlWhenProofImageIdProvided() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            FileRecord proofFile = new FileRecord();
            proofFile.setId(100L);
            proofFile.setFileUrl("/uploads/proofs/test.jpg");
            when(fileService.requireOwnedFile(eq(100L), any())).thenReturn(proofFile);

            OrderCompleteRequest request = new OrderCompleteRequest();
            request.setProofImageId(100L);
            request.setNote("Done with proof");

            orderService.completeOrder(1L, request);

            assertThat(order.getCompletionProofUrl()).isEqualTo("/uploads/proofs/test.jpg");
        }
    }

    // ==================== confirmCompletion ====================
    @Nested
    class ConfirmCompletionTests {

        @Test
        void shouldConfirmCompletionWhenPublisherPendingCompletion() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.PENDING_COMPLETION);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(taskMapper.selectById(10L)).thenReturn(createTask(10L, CURRENT_USER_ID, "Test Task"));

            orderService.confirmCompletion(1L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            verify(orderMapper).updateById(order);
            verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
            verify(notificationService).createOrderStatusNotification(eq(OTHER_USER_ID), eq(1L), eq(OrderStatus.COMPLETED));
            verify(notificationService, times(2)).createReviewRequestNotification(anyLong(), eq(1L), eq("Test Task"));
        }

        @Test
        void shouldThrowWhenNotPublisher() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.PENDING_COMPLETION);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.confirmCompletion(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非订单参与方");
        }

        @Test
        void shouldThrowWhenNotPendingCompletionStatus() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.confirmCompletion(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单状态不允许");
        }
    }

    // ==================== cancelOrder ====================
    @Nested
    class CancelOrderTests {

        @Test
        void shouldCancelFromInProgressAsPublisher() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            Task task = createTask(10L, CURRENT_USER_ID, "Test task");
            task.setStatus(TaskStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(taskMapper.selectById(10L)).thenReturn(task);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("No longer needed");

            orderService.cancelOrder(1L, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRM);
            assertThat(order.getCancelReason()).isNull();
            assertThat(order.getServiceProviderId()).isNull();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
            verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
            verify(taskMapper).updateById(task);
            verify(applicationMapper).update(isNull(), any());
            verify(notificationService).createOrderStatusNotification(eq(OTHER_USER_ID), eq(1L), eq(OrderStatus.PENDING_CONFIRM));
        }

        @Test
        void shouldSubmitCancelRequestFromInProgressAsServiceProvider() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Cannot fulfill");

            orderService.cancelOrder(1L, request);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
            assertThat(order.getCancelReason()).isEqualTo("Cannot fulfill");
            ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
            verify(orderStatusLogMapper).insert(logCaptor.capture());
            assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(OrderStatus.IN_PROGRESS.name());
            assertThat(logCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.IN_PROGRESS.name());
            verify(notificationService).createOrderActionNotification(eq(OTHER_USER_ID), eq(1L), eq("服务方申请取消服务"), eq("服务方申请取消订单，原因：Cannot fulfill"));
        }

        @Test
        void shouldApproveCancelRequestAndReopenTask() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            order.setCancelReason("Cannot fulfill");
            Task task = createTask(10L, CURRENT_USER_ID, "Test task");
            task.setStatus(TaskStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(taskMapper.selectById(10L)).thenReturn(task);

            orderService.approveCancelRequest(1L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRM);
            assertThat(order.getCancelReason()).isNull();
            assertThat(order.getServiceProviderId()).isNull();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
            verify(applicationMapper).update(isNull(), any());
            verify(notificationService).createOrderActionNotification(eq(OTHER_USER_ID), eq(1L), anyString(), anyString());
        }

        @Test
        void shouldRejectCancelRequestAndKeepOrderInProgress() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            order.setCancelReason("Cannot fulfill");
            when(orderMapper.selectById(1L)).thenReturn(order);

            orderService.rejectCancelRequest(1L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
            assertThat(order.getCancelReason()).isNull();
            verify(notificationService).createOrderActionNotification(eq(OTHER_USER_ID), eq(1L), eq("发布方已拒绝取消申请"), eq("发布方已拒绝取消申请，订单继续进行"));
        }

        @Test
        void shouldThrowWhenAlreadyCancelled() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.CANCELLED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Try again");

            assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单已取消");
        }

        @Test
        void shouldThrowWhenAlreadyCompleted() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Try again");

            assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单已完成");
        }

        @Test
        void shouldThrowWhenNotParticipant() {
            Order order = createOrder(1L, 10L, 99999L, 88888L, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Try");

            assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非订单参与方");
        }

        @Test
        void shouldThrowWhenPendingConfirm() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.PENDING_CONFIRM);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Try again");

            assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单已退回待接单状态");
        }

        @Test
        void shouldThrowWhenDuplicateCancelRequest() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.IN_PROGRESS);
            order.setCancelReason("Cannot fulfill");
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Try again");

            assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已提交取消申请");
        }
    }

    // ==================== submitReview ====================
    @Nested
    class SubmitReviewTests {

        @Test
        void shouldSubmitReviewSuccessfully() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(reviewMapper.selectCount(any())).thenReturn(0L);
            when(creditLogMapper.selectOne(any())).thenReturn(null);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setRating(5);
            request.setContent("Great service!");

            orderService.submitReview(1L, request);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewMapper).insert(captor.capture());
            Review saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(1L);
            assertThat(saved.getReviewerId()).isEqualTo(CURRENT_USER_ID);
            assertThat(saved.getRevieweeId()).isEqualTo(OTHER_USER_ID);
            assertThat(saved.getRating()).isEqualTo(5);
            assertThat(saved.getContent()).isEqualTo("Great service!");
            verify(creditLogMapper).insert(any(CreditLog.class));
        }

        @Test
        void shouldThrowWhenDuplicateReview() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(reviewMapper.selectCount(any())).thenReturn(1L);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setRating(4);
            request.setContent("Again?");

            assertThatThrownBy(() -> orderService.submitReview(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已提交过评价");
        }

        @Test
        void shouldThrowWhenOrderNotCompleted() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setRating(4);
            request.setContent("Too early");

            assertThatThrownBy(() -> orderService.submitReview(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单未完成");
        }

        @Test
        void shouldThrowWhenNotParticipant() {
            Order order = createOrder(1L, 10L, 99999L, 88888L, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setRating(3);
            request.setContent("Not my order");

            assertThatThrownBy(() -> orderService.submitReview(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非订单参与方");
        }
    }

    // ==================== listOrders ====================
    @Nested
    class ListOrdersTests {

        @Test
        void shouldListOrdersAsPublisher() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            Page<Order> orderPage = new Page<>(1, 20);
            orderPage.setRecords(List.of(order));
            orderPage.setTotal(1);
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(orderPage);
            when(taskMapper.selectById(10L)).thenReturn(createTask(10L, CURRENT_USER_ID, "Test Task"));

            PageResult<OrderItemVO> result = orderService.listOrders(1, 20, "PUBLISHER", null, null);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        void shouldListOrdersFilteredByStatus() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.COMPLETED);
            Page<Order> orderPage = new Page<>(1, 20);
            orderPage.setRecords(List.of(order));
            orderPage.setTotal(1);
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(orderPage);
            when(taskMapper.selectById(10L)).thenReturn(createTask(10L, CURRENT_USER_ID, "Test Task"));

            PageResult<OrderItemVO> result = orderService.listOrders(1, 20, null, OrderStatus.COMPLETED, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }
    }

    // ==================== listReviews ====================
    @Nested
    class ListReviewsTests {

        @Test
        void shouldReturnReviewsForOrder() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            Review review = new Review();
            review.setId(1L);
            review.setOrderId(1L);
            review.setReviewerId(CURRENT_USER_ID);
            review.setRevieweeId(OTHER_USER_ID);
            review.setRating(5);
            review.setContent("Excellent");
            review.setCreatedAt(LocalDateTime.now());
            when(reviewMapper.selectList(any())).thenReturn(List.of(review));

            List<ReviewItemVO> result = orderService.listReviews(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRating()).isEqualTo(5);
        }
    }

    // ==================== state machine ====================
    @Nested
    class StateMachineTests {

        @Test
        void fullOrderLifecycleShouldWork() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            // Step 1: Service provider completes
            FileRecord proofFile = new FileRecord();
            proofFile.setId(100L);
            proofFile.setFileUrl("/uploads/proofs/test.jpg");
            when(fileService.requireOwnedFile(eq(100L), any())).thenReturn(proofFile);

            OrderCompleteRequest completeRequest = new OrderCompleteRequest();
            completeRequest.setProofImageId(100L);
            completeRequest.setNote("Done");
            securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(OTHER_USER_ID);
            orderService.completeOrder(1L, completeRequest);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_COMPLETION);

            // Step 2: Publisher confirms completion
            securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(CURRENT_USER_ID);
            when(taskMapper.selectById(10L)).thenReturn(createTask(10L, CURRENT_USER_ID, "Task"));
            orderService.confirmCompletion(1L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        void cancelFromAnyNonFinalStateShouldWork() {
            Order order = createOrder(1L, 10L, CURRENT_USER_ID, OTHER_USER_ID, OrderStatus.PENDING_COMPLETION);
            Task task = createTask(10L, CURRENT_USER_ID, "Test task");
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(taskMapper.selectById(10L)).thenReturn(task);

            OrderCancelRequest request = new OrderCancelRequest();
            request.setReason("Changed mind");

            orderService.cancelOrder(1L, request);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRM);
            assertThat(order.getServiceProviderId()).isNull();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
        }

        @Test
        void shouldNotSkipStatusInNormalFlow() {
            Order order = createOrder(1L, 10L, OTHER_USER_ID, CURRENT_USER_ID, OrderStatus.IN_PROGRESS);
            when(orderMapper.selectById(1L)).thenReturn(order);

            // Service provider tries to confirmCompletion (only publisher can)
            assertThatThrownBy(() -> orderService.confirmCompletion(1L))
                    .isInstanceOf(BusinessException.class);

            // Status should not have changed
            assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        }
    }
}
