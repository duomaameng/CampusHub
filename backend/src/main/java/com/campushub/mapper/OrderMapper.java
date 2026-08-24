package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("""
            UPDATE orders
            SET status = 'TIMEOUT', cancel_reason = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE task_id = #{taskId} AND status = 'PENDING_CONFIRM'
            """)
    int timeoutPendingConfirmOrderByTaskId(@Param("taskId") Long taskId);

    @Update("""
            UPDATE orders
            SET status = 'TIMEOUT', cancel_reason = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE status = 'PENDING_CONFIRM'
              AND task_id IN (
                  SELECT id FROM task
                  WHERE status = 'EXPIRED' AND deadline <= CURRENT_TIMESTAMP
              )
            """)
    int timeoutPendingConfirmOrdersForExpiredTasks();

    @Update("""
            UPDATE orders
            SET status = 'TIMEOUT', cancel_reason = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE task_id = #{taskId} AND status = 'IN_PROGRESS'
            """)
    int timeoutInProgressOrderByTaskId(@Param("taskId") Long taskId);

    @Update("""
            UPDATE orders
            SET status = 'TIMEOUT', cancel_reason = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE status = 'IN_PROGRESS'
              AND task_id IN (
                  SELECT id FROM task
                  WHERE status = 'IN_PROGRESS' AND deadline <= CURRENT_TIMESTAMP
              )
            """)
    int timeoutInProgressOrdersPastTaskDeadline();
}
