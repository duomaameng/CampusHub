package com.campushub.service;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${campus-hub.mail.delivery-mode:smtp}")
    private String deliveryMode;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${campus-hub.mail.from:}")
    private String configuredFrom;

    public void sendVerificationCode(String to, String code, String purpose) {
        if ("log".equalsIgnoreCase(deliveryMode)) {
            log.info("Verification code generated for {} with purpose {}: {}", to, purpose, code);
            return;
        }
        if (!"smtp".equalsIgnoreCase(deliveryMode)) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, "邮件发送模式配置无效，请使用 smtp 或 log");
        }

        String action = "RESET_PASSWORD".equals(purpose) ? "重置密码" : "注册账号";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(to);
        message.setSubject("CampusHub 邮箱验证码");
        message.setText("""
                您好：

                您正在进行 CampusHub %s 操作，验证码为：

                %s

                验证码 10 分钟内有效，请勿泄露给他人。
                如果不是您本人操作，请忽略本邮件。
                """.formatted(action, code));

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", to);
        } catch (MailException e) {
            log.warn("Failed to send verification email to {}: {}", to, e.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, "验证码邮件发送失败，请检查邮箱服务配置后重试");
        }
    }

    private String resolveFromAddress() {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, "未配置 SMTP 发件账号，请设置 MAIL_USERNAME 和 MAIL_PASSWORD");
        }
        if (StringUtils.hasText(configuredFrom)) {
            return configuredFrom;
        }
        return mailUsername;
    }
}
