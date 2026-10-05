package com.moj.userservice.Controller;

import com.moj.userservice.Dto.AddAvatarDto;
import com.moj.userservice.Dto.AddProductDto;
import com.moj.userservice.Dto.BusinessDetailsDto;
import com.moj.userservice.Dto.UpdateAvatarDto;
import com.moj.userservice.Response.ProductDetailsResponse;
import com.moj.userservice.Response.UserDetailsResponse;
import com.moj.userservice.Service.UserService;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(value = "/add-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProduct(@RequestHeader("X-USER-ID") UUID userId, @RequestPart AddProductDto addProductDto,@RequestPart(value = "file", required = false) MultipartFile file){
        userService.addProduct(file, addProductDto, userId);
        return ResponseEntity.ok().build();
    }
    @PutMapping(value = "/update-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(@RequestHeader("X-USER-ID") UUID userId,@RequestParam Long productId ,@RequestPart AddProductDto addProductDto,@RequestPart(value = "file", required = false) MultipartFile file){
        userService.updateProduct(userId, productId, addProductDto, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get-user-product-details")
    public ResponseEntity<Page<ProductDetailsResponse>> getUserProductDetails(@RequestHeader("X-USER-ID") UUID userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<ProductDetailsResponse> response = userService.getProduct(userId, page, size);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/delete-product")
    public ResponseEntity<?> deleteProduct(@RequestHeader("X-USER-ID") UUID userId, @RequestParam Long productId){
        userService.deleteProduct(userId, productId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/add-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProduct(@RequestHeader("X-USER-ID") UUID userId, @RequestPart MultipartFile file, @RequestPart AddAvatarDto addAvatarDto){
        userService.addAvatar(file, addAvatarDto, userId);
        return ResponseEntity.ok().build();
    }
    @PutMapping(value = "/update-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(@RequestHeader("X-USER-ID") UUID userId, @RequestPart MultipartFile file, @RequestPart UpdateAvatarDto updateAvatarDto){
        userService.updateAvatar(userId, updateAvatarDto, file);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/delete-avatar")
    public ResponseEntity<?> deleteAvatar(@RequestHeader("X-USER-ID") UUID userId, @RequestParam Long avatarId){
        userService.deleteAvatar(userId, avatarId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/get-avatars")
    public ResponseEntity<?>getAvatars(@RequestHeader("X-USER-ID") UUID userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size){
        return ResponseEntity.ok(userService.getAvatars(userId, page, size));
    }

}
