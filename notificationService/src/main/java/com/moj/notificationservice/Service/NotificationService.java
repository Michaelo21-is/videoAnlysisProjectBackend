package com.moj.notificationservice.Service;

import com.moj.notificationservice.Configuration.RabbitMqConfig;
import com.moj.notificationservice.Dto.TwoFactorEmailDataDto;
import com.moj.notificationservice.Util.GetMessageByTwoFactorType;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final String resendApiKey;
    public NotificationService(@Value("${resend.api.key}") String resendApiKey) {
        this.resendApiKey = resendApiKey;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void sendEmail(TwoFactorEmailDataDto twoFactorEmailDataDto) {
        Resend resend = new Resend(resendApiKey);
        String message = GetMessageByTwoFactorType.getMessage(twoFactorEmailDataDto.getTwoFactorType(), twoFactorEmailDataDto.getVerificationCode());
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
}