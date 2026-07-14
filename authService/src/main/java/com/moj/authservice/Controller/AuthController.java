package com.moj.authservice.Controller;

import com.moj.authservice.Dto.SignInDto;
import com.moj.authservice.Dto.SignUpDto;
import com.moj.authservice.Enums.TwoFactorType;
import com.moj.authservice.Response.AuthResponse;
import com.moj.authservice.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }



    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpDto signUpDto) {
        AuthResponse authResponse = authService.signUp(signUpDto);
        return ResponseEntity.status(authResponse.getStatus()).body(authResponse.getMessage());
    }



    @PostMapping("/sign-in")
    public ResponseEntity<?>signIn(@RequestBody SignInDto signInDto) {
        AuthResponse authResponse = authService.signIn(signInDto);
        return ResponseEntity.status(authResponse.getStatus()).body(authResponse.getMessage());
    }



    @PostMapping("/verify-2fa")
    public ResponseEntity<?>verify2Factor(@RequestParam("verification-code") Integer verificationCode, @RequestHeader("X-USER-ID") UUID userId){
        AuthResponse authResponse = authService.verifyTwoFactor(verificationCode, userId);
        return ResponseEntity.status(authResponse.getStatus()).body(authResponse.getMessage());
    }



    @PostMapping("/renew_2fa")
    public ResponseEntity<String>renew2Factor(@RequestHeader("X-USER-ID") UUID userId, @RequestParam("two-factor-type")TwoFactorType twoFactorType){
        authService.setTwoFactor(userId, twoFactorType);
        return ResponseEntity.ok("two factor has been renewed, check your email.");
    }



    @PostMapping("/forgot-my-password-request")
    public ResponseEntity<?>forgotPassword(@RequestParam("email") String email){
        AuthResponse authResponse = authService.restPasswordRequest(email);
        return ResponseEntity.status(authResponse.getStatus()).body(authResponse.getMessage());
    }



    @PostMapping("/set-new-password")
    public ResponseEntity<?>setNewPassword(@RequestParam("password") String password, @RequestHeader("X-USER-ID") UUID userId){
        AuthResponse authResponse = authService.setNewPassword(userId, password);
        return ResponseEntity.status(authResponse.getStatus()).body(authResponse.getMessage());
    }
}
