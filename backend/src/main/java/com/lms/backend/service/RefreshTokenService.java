package com.lms.backend.service;

import com.lms.backend.entity.AppUser;
import com.lms.backend.entity.RefreshToken;
import com.lms.backend.exception.DomainExceptions.BusinessRuleException;
import com.lms.backend.repository.AppUserRepository;
import com.lms.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public RefreshToken createRefreshToken(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        // 1. Forcefully delete any existing token for this user
        refreshTokenRepository.deleteByUser(user);

        // 2. Create the new token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 days
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public AppUser verifyAndGetUser(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BusinessRuleException("Refresh token is not in database!"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BusinessRuleException("Refresh token was expired. Please make a new signin request");
        }

        return token.getUser();
    }
}