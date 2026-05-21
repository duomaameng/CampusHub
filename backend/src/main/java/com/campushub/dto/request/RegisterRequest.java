package com.campushub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 32, message = "Password length must be between 8 and 32")
    private String password;

    @NotBlank(message = "Confirm password must not be blank")
    private String confirmPassword;

    @NotBlank(message = "Verification code must not be blank")
    private String code;
}
