package com.moj.authservice.Controller;

import com.moj.authservice.Dto.SignInDto;
import com.moj.authservice.Dto.SignUpDto;
import com.moj.authservice.Enums.TokenType;
import com.moj.authservice.Response.AuthResponse;
import com.moj.authservice.Service.AuthService;
import com.moj.authservice.Service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
}
