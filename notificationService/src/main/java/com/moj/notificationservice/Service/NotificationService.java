package com.moj.notificationservice.Service;

import com.moj.notificationservice.Configuration.RabbitMqConfig;
import com.moj.notificationservice.Dto.TwoFactorEmailDataDto;
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
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Acme <onboarding@resend.dev>")
                .to("delivered@resend.dev")
                .subject("2 fa verification code")
                .html("<h1>Your verification code is: " + twoFactorEmailDataDto.getVerificationCode() + "</h1>")
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }
}