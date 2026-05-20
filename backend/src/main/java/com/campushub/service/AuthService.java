package com.campushub.service;

import com.campushub.dto.request.*;
import com.campushub.dto.response.LoginResponse;
import com.campushub.dto.response.RegisterResponse;
import com.campushub.dto.response.VerifyEmailResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void sendVerificationCode(SendVerificationCodeRequest request);

    VerifyEmailResponse verifyEmail(VerifyEmailRequest request);

    void resetPassword(ResetPasswordRequest request);
}
