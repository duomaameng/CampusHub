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
}
