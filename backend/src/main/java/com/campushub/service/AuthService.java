package com.campushub.service;

import com.campushub.dto.request.*;
import com.campushub.vo.auth.LoginVO;
import com.campushub.vo.auth.RegisterVO;
import com.campushub.vo.auth.VerifyEmailVO;

public interface AuthService {

    RegisterVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    void sendVerificationCode(SendVerificationCodeRequest request);

    VerifyEmailVO verifyEmail(VerifyEmailRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(String token);
}
