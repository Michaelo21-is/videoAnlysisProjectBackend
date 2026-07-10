package com.moj.notificationservice.Controller;

import com.moj.notificationservice.Dto.TwoFactorEmailDataDto;
import com.moj.notificationservice.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    @PostMapping("/send-email-for-2fa")
    public ResponseEntity<?> sendEmailFor2FA(@RequestBody TwoFactorEmailDataDto twoFactorEmailDataDto) {
        notificationService.sendEmail(twoFactorEmailDataDto);
        return ResponseEntity.ok().build();
    }
}
