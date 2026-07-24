package com.moj.userservice.Controller;

import com.moj.userservice.Response.UserDetailsResponse;
import com.moj.userservice.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/details")
    public ResponseEntity<UserDetailsResponse> getUserDetails(@RequestHeader("X-USER-ID") UUID userId) {
        UserDetailsResponse userDetailsResponse = userService.getUserDetails(userId);
        return ResponseEntity.ok(userDetailsResponse);
    }
}
