package com.moj.notificationservice.Service;

import com.moj.notificationservice.Configuration.RabbitMqConfig;
import com.moj.notificationservice.Dto.OrderCreditDto;
import com.moj.notificationservice.Dto.ResetPasswordDto;
import com.moj.notificationservice.Dto.TwoFactorEmailDataDto;
import com.moj.notificationservice.Util.GetMessage;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class NotificationService {

    private final String resendApiKey;
    private final GetMessage getMessage;
    public NotificationService(@Value("${resend.api.key}") String resendApiKey, GetMessage getMessage) {
        this.resendApiKey = resendApiKey;
        this.getMessage = getMessage;
    }

    @RabbitListener(queues = RabbitMqConfig.TWO_FACTOR_QUEUE)
    public void sendEmail(TwoFactorEmailDataDto twoFactorEmailDataDto) {
        Resend resend = new Resend(resendApiKey);
        String message = getMessage.getMessageByTwoFactor(twoFactorEmailDataDto.getTwoFactorType(), twoFactorEmailDataDto.getVerificationCode());
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Acme <onboarding@resend.dev>")
                .to(twoFactorEmailDataDto.getEmail())
                .subject("2 factor verification code")
                .html("<h1>" + message + "</h1>")
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send two-factor email", e);
        }
    }
    @RabbitListener(queues = RabbitMqConfig.PASSWORD_RESET_QUEUE)
    public void sendPasswordResetEmail(ResetPasswordDto resetPasswordDto) {
        Resend resend = new Resend(resendApiKey);
        String message = getMessage.getMessageByPasswordReset(resetPasswordDto.getResetToken());
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Acme <onboarding@resend.dev>")
                .to(resetPasswordDto.getEmail())
                .subject("reset password")
                .html("<h1>" + message + "</h1>")
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send two-factor email", e);
        }
    }
//    @RabbitListener(queues = RabbitMqConfig.ORDER_CREDIT_QUEUE) need to change queue when user is sucessed
    public void sendOrderCreditEmail(OrderCreditDto orderCreditDto) {
        Resend resend = new Resend(resendApiKey);
        String message =
    }
}