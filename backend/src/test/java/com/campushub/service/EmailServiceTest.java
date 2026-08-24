package com.campushub.service;

import com.campushub.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @Test
    void shouldNotSendSmtpMailInLogMode() {
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "deliveryMode", "log");

        emailService.sendVerificationCode("student.demo@smail.nju.edu.cn", "123456", "REGISTER");

        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void shouldRejectInvalidDeliveryMode() {
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "deliveryMode", "invalid");

        assertThatThrownBy(() -> emailService.sendVerificationCode("student.demo@smail.nju.edu.cn", "123456", "REGISTER"))
                .isInstanceOf(BusinessException.class);
    }
}
