package com.lms.backend.controller;

import com.lms.backend.entity.AppUser;
import com.lms.backend.entity.RefreshToken;
import com.lms.backend.security.JwtService;
import com.lms.backend.service.AuthService;
import com.lms.backend.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    // DTOs
    public record RegisterRequest(@Email String email, @NotBlank String password, String firstName, String lastName) {}
    public record LoginRequest(@Email String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, String refreshToken, String role) {}
    public record TokenRefreshRequest(@NotBlank String refreshToken) {}
    public record ForgotPasswordRequest(@Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank String password) {}

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.firstName(), request.lastName());
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AppUser user = authService.findByEmail(request.email());
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), user.getRole().name()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        // The controller just calls the service and maps the result. No logic here.
        AppUser user = refreshTokenService.verifyAndGetUser(request.refreshToken());
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());

        return ResponseEntity.ok(new AuthResponse(newAccessToken, request.refreshToken(), user.getRole().name()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.createPasswordResetToken(request.email());
        return ResponseEntity.ok("If an account with that email exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok("Password successfully reset. You can now log in.");
    }
}