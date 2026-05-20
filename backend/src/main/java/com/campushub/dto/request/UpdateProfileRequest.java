package com.campushub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 30, message = "昵称最长 30 个字符")
    private String nickname;

    private String avatarUrl;

    private String gender;

    private String grade;

    private String college;

    @Size(max = 200, message = "个人简介最长 200 个字符")
    private String bio;

    private String campus;

    private String contact;

    private Boolean contactVisible;
}
