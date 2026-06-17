package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Update("""
            UPDATE task
            SET status = #{nextStatus}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{taskId} AND status = #{currentStatus}
            """)
    int updateStatusIfCurrent(@Param("taskId") Long taskId,
                              @Param("currentStatus") String currentStatus,
                              @Param("nextStatus") String nextStatus);

    @Update("""
            UPDATE task
            SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP
            WHERE status = 'OPEN' AND deadline <= CURRENT_TIMESTAMP
            """)
    int expireOpenTasksPastDeadline();

    @Update("""
            UPDATE task
            SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP
            WHERE status = 'IN_PROGRESS'
              AND deadline <= CURRENT_TIMESTAMP
              AND id IN (
                  SELECT task_id FROM orders
                  WHERE status = 'TIMEOUT'
              )
            """)
    int expireInProgressTasksWithTimedOutOrders();
}
