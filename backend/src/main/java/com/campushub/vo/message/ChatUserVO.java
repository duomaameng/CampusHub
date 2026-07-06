package com.campushub.vo.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatUserVO {
    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;
}
