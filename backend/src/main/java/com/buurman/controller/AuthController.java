package com.buurman.controller;

import com.buurman.dto.request.RegisterRequest;
import com.buurman.dto.request.UpdateProfileRequest;
import com.buurman.dto.request.VerifyEmailRequest;
import com.buurman.dto.response.UserResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration and profile management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register new user", description = "Create user account, team, and assign as admin")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Get current user", description = "Get authenticated user profile",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.getCurrentUser(principal.getUserId());
    }

    @Operation(summary = "Update profile", description = "Update current user's profile",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(principal.getUserId(), request);
    }

    @Operation(summary = "Verify email", description = "Verify email address with 6-digit code",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/verify-email")
    public UserResponse verifyEmail(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(principal.getUserId(), request.code());
    }

    @Operation(summary = "Resend verification code", description = "Resend email verification code",
               security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@AuthenticationPrincipal UserPrincipal principal) {
        authService.resendVerificationCode(principal.getUserId());
    }
}
