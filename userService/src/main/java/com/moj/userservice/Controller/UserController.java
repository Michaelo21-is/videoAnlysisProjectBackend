package com.moj.userservice.Controller;

import com.moj.userservice.Dto.BusinessDetailsDto;
import com.moj.userservice.Response.UserDetailsResponse;
import com.moj.userservice.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/set-business-details")
    public ResponseEntity<?> setBusinessDetails(@RequestHeader("X-USER-ID") UUID userId, @RequestBody BusinessDetailsDto businessDetailsDto) {
        userService.setBusinessDetails(userId, businessDetailsDto);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/get-business-details")
    public ResponseEntity<BusinessDetailsDto> getBusinessDetails(@RequestHeader("X-USER-ID") UUID userId){
        BusinessDetailsDto businessDetailsDto = userService.getBusinessDetails(userId);
        return ResponseEntity.ok(businessDetailsDto);
    }
    @PutMapping("/update-business-details")
    public ResponseEntity<?> updateBusinessDetails(@RequestHeader("X-USER-ID") UUID userId, @RequestBody BusinessDetailsDto businessDetailsDto){
        userService.updateBusinessDetails(userId, businessDetailsDto);
        return ResponseEntity.ok().build();
    }
}
