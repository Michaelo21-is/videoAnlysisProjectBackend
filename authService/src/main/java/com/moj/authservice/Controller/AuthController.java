package com.moj.authservice.Controller;

import com.moj.authservice.Util.CookieUtil;
import com.moj.authservice.Dto.SignInDto;
import com.moj.authservice.Dto.SignUpDto;
import com.moj.authservice.Enums.TwoFactorType;
import com.moj.authservice.Response.RegularResponse;
import com.moj.authservice.Response.AccessAndRefreshResponse;
import com.moj.authservice.Response.SignInResponse;
import com.moj.authservice.Response.TempTokenResponse;
import com.moj.authservice.Service.AuthService;
import com.moj.authservice.Service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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
        SignInResponse signInResponse = authService.signIn(signInDto);
        if (signInResponse.getAccessToken() != null && signInResponse.getRefreshToken() != null){
            ResponseCookie cookie = cookieUtil.createJwtCookie(CookieUtil.accessToken, signInResponse.getAccessToken(), JwtService.ACCESS_TOKEN_EXPIRATION);
            ResponseCookie refreshCookie = cookieUtil.createJwtCookie(CookieUtil.refreshToken, signInResponse.getRefreshToken(), JwtService.REFRESH_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(signInResponse.getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(signInResponse.getMessage());
        }
        if (signInResponse.getTempToken() != null){
            ResponseCookie tempCookie = cookieUtil.createJwtCookie(CookieUtil.tempToken, signInResponse.getTempToken(), JwtService.TEMP_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(signInResponse.getStatus())
                    .header(HttpHeaders.SET_COOKIE, tempCookie.toString())
                    .body(Map.of(
                            "message", signInResponse.getMessage(),
                            "requiresEmailVerification",
                            signInResponse.isRequiresEmailVerification()
                    ));
        }
        return ResponseEntity.status(signInResponse.getStatus()).body(signInResponse.getMessage());
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
        RegularResponse regularResponse = authService.restPasswordRequest(email);
        return ResponseEntity.status(regularResponse.getStatus()).body(regularResponse.getMessage());
    }

    @GetMapping("/password-reset/validate")
    public ResponseEntity<Boolean>checkUserPermissionForRestPassword(@RequestHeader("X-Reset-Token") String resetToken){
        Boolean response = authService.isResetTokenValid(resetToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/set-new-password")
    public ResponseEntity<?>setNewPassword(@RequestParam("password") String password, @RequestHeader("X-Reset-Token") String resetToken){
        RegularResponse regularResponse = authService.setNewPassword(resetToken, password);
        return ResponseEntity.status(regularResponse.getStatus()).body(regularResponse.getMessage());
    }

    @GetMapping("/two-factor/status")
    public ResponseEntity<Boolean>checkPagePermission(@RequestHeader("X-USER-ID") UUID userId, @RequestParam("email") String email){
        boolean allowed = authService.isUserAllowedOnTwoFactorPage(email, userId);
        return ResponseEntity.ok(allowed);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh-token", required = false) String refreshToken) {
        AccessAndRefreshResponse response = authService.refreshToken(refreshToken);

        if (response.getAccessToken() != null && response.getRefreshToken() != null) {
            ResponseCookie accessCookie = cookieUtil.createJwtCookie(CookieUtil.accessToken, response.getAccessToken(), JwtService.ACCESS_TOKEN_EXPIRATION);
            ResponseCookie refreshCookie = cookieUtil.createJwtCookie(CookieUtil.refreshToken, response.getRefreshToken(), JwtService.REFRESH_TOKEN_EXPIRATION);
            return ResponseEntity
                    .status(response.getStatus())
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(response.getMessage());
        }

        return ResponseEntity
                .status(response.getStatus())
                .body(response.getMessage());
    }
    @DeleteMapping("/sign-out")
    public ResponseEntity<?> signOut(@RequestHeader("X-USER-ID") UUID userId){
        authService.signOut(userId);
        ResponseCookie clearTempCookie = cookieUtil.deleteCookie(CookieUtil.tempToken);
        ResponseCookie clearAccessCookie = cookieUtil.deleteCookie(CookieUtil.accessToken);
        ResponseCookie clearRefreshCookie = cookieUtil.deleteCookie(CookieUtil.refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, clearTempCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body("signed out successfully");
    }
}
