package com.campushub.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.enums.NotificationType;
import com.campushub.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("""
            <script>
            SELECT *
            FROM notification
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              <if test="read != null">
                AND is_read = #{read}
              </if>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    IPage<Notification> selectUserNotifications(IPage<Notification> page,
                                                @Param("receiverId") Long receiverId,
                                                @Param("read") Boolean read);

    @Select("""
            SELECT COUNT(*)
            FROM notification
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            """)
    long countUnread(@Param("receiverId") Long receiverId);
}
