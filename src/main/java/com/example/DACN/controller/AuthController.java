package com.example.DACN.controller;

import com.example.DACN.dto.request.ChangePasswordRequest;
import com.example.DACN.dto.request.ForgotPasswordRequest;
import com.example.DACN.dto.request.LoginRequest;
import com.example.DACN.dto.request.RegisterRequest;
import com.example.DACN.dto.request.ResetPasswordRequest;
import com.example.DACN.dto.response.ChangePasswordResponse;
import com.example.DACN.dto.response.ForgotPasswordResponse;
import com.example.DACN.dto.response.LoginResponse;
import com.example.DACN.dto.response.RegisterResponse;
import com.example.DACN.dto.response.ResetPasswordResponse;
import com.example.DACN.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new customer account with email, password, full name, and phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failed)"),
            @ApiResponse(responseCode = "409", description = "Email or phone number already exists")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and generate JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account banned")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset OTP code to email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset code sent successfully", content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failed)")
    })
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using OTP code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or validation failed")
    })
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password", description = "Change password for authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(schema = @Schema(implementation = ChangePasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Incorrect old password or unauthorized")
    })
    public ResponseEntity<ChangePasswordResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = authentication.getName();
        ChangePasswordResponse response = authService.changePassword(email, request);
        return ResponseEntity.ok(response);
    }
}
