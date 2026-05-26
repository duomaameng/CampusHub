package com.campushub.vo.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnnouncementPublishVO {

    private Long announcementId;

    private String status;
}
