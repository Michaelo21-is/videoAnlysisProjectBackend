package com.moj.authservice.Controller;

import com.moj.authservice.Component.CookieUtil;
import com.moj.authservice.Dto.SignInDto;
import com.moj.authservice.Dto.SignUpDto;
import com.moj.authservice.Enums.TwoFactorType;
import com.moj.authservice.Response.RegularResponse;
import com.moj.authservice.Response.AccessAndRefreshResponse;
import com.moj.authservice.Response.TempTokenResponse;
import com.moj.authservice.Service.AuthService;
import com.moj.authservice.Service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;
    private final CookieUtil cookieUtil;
    public AuthController(AuthService authService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
    }



    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpDto signUpDto) {
        TempTokenResponse tempTokenResponse = authService.signUp(signUpDto);
        if (tempTokenResponse.getTempToken() != null){
            ResponseCookie cookie = cookieUtil.createJwtCookie(CookieUtil.tempToken, tempTokenResponse.getTempToken(), JwtService.TEMP_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(tempTokenResponse.getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(tempTokenResponse.getMessage());
        }
        return ResponseEntity
                .status(tempTokenResponse.getStatus())
                .body(tempTokenResponse.getMessage());
    }



    @PostMapping("/sign-in")
    public ResponseEntity<?>signIn(@RequestBody SignInDto signInDto) {
        AccessAndRefreshResponse accessAndRefreshResponse = authService.signIn(signInDto);
        if (accessAndRefreshResponse.getAccessToken() != null && accessAndRefreshResponse.getRefreshToken() != null){
            ResponseCookie cookie = cookieUtil.createJwtCookie(CookieUtil.accessToken, accessAndRefreshResponse.getAccessToken(), JwtService.ACCESS_TOKEN_EXPIRATION);
            ResponseCookie refreshCookie = cookieUtil.createJwtCookie(CookieUtil.refreshToken, accessAndRefreshResponse.getRefreshToken(), JwtService.REFRESH_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(accessAndRefreshResponse.getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(accessAndRefreshResponse.getMessage());
        }
        return ResponseEntity.status(accessAndRefreshResponse.getStatus()).body(accessAndRefreshResponse.getMessage());
    }



    @PostMapping("/verify-2fa")
    public ResponseEntity<?>verify2Factor(@RequestParam("verification-code") Integer verificationCode, @RequestHeader("X-USER-ID") UUID userId){
        AccessAndRefreshResponse regularResponse = authService.verifyTwoFactor(verificationCode, userId);
        if (regularResponse.getAccessToken() != null && regularResponse.getRefreshToken() != null){
            ResponseCookie clearTempCookie = cookieUtil.deleteCookie(CookieUtil.tempToken);
            ResponseCookie cookie = cookieUtil.createJwtCookie(CookieUtil.accessToken, regularResponse.getAccessToken(), JwtService.ACCESS_TOKEN_EXPIRATION);
            ResponseCookie refreshCookie = cookieUtil.createJwtCookie(CookieUtil.refreshToken, regularResponse.getRefreshToken(), JwtService.REFRESH_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(regularResponse.getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, clearTempCookie.toString())
                    .body(regularResponse.getMessage());
        }
        return ResponseEntity.status(regularResponse.getStatus()).body(regularResponse.getMessage());
    }



    @PostMapping("/renew-2fa")
    public ResponseEntity<String>renew2Factor(@RequestHeader("X-USER-ID") UUID userId, @RequestParam("two-factor-type")TwoFactorType twoFactorType){
        authService.setTwoFactor(userId, twoFactorType);
        return ResponseEntity.ok("two factor has been renewed, check your email.");
    }



    @PostMapping("/forgot-my-password-request")
    public ResponseEntity<?>forgotPassword(@RequestParam("email") String email){
        TempTokenResponse regularResponse = authService.restPasswordRequest(email);
        if (regularResponse.getTempToken() != null){
            ResponseCookie cookie = cookieUtil.createJwtCookie(CookieUtil.tempToken, regularResponse.getTempToken(), JwtService.TEMP_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(regularResponse.getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(regularResponse.getMessage());
        }
        return ResponseEntity.status(regularResponse.getStatus()).body(regularResponse.getMessage());
    }



    @PostMapping("/set-new-password")
    public ResponseEntity<?>setNewPassword(@RequestParam("password") String password, @RequestHeader("X-USER-ID") UUID userId){
        RegularResponse regularResponse = authService.setNewPassword(userId, password);
        return ResponseEntity.status(regularResponse.getStatus()).body(regularResponse.getMessage());
    }
}
