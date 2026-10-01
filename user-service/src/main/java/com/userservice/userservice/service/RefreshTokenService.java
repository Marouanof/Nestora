package com.userservice.userservice.service;

import com.userservice.userservice.dto.SessionResponse;
import com.userservice.userservice.entity.RefreshToken;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.InvalidTokenException;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        return createRefreshToken(userId, null, null);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId, String userAgent, String ipAddress) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Multi-session : on ne supprime plus l'ancien token, chaque login = nouvelle session
        RefreshToken refreshToken = new RefreshToken(user, userAgent, ipAddress);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token was revoked. Please make a new login request");
        }
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token was expired. Please make a new login request");
        }
        token.setLastUsedAt(LocalDateTime.now());
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Transactional
    public void revokeUserToken(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Legacy single-session : révoque le premier token trouvé (conservé pour compat)
        refreshTokenRepository.findByUser(user).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(Long userId, String currentToken) {
        return refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> new SessionResponse(
                        t.getToken(),
                        t.getCreatedAt(),
                        t.getLastUsedAt(),
                        t.getIpAddress(),
                        t.getUserAgent(),
                        currentToken != null && currentToken.equals(t.getToken()),
                        t.isRevoked()))
                .toList();
    }

    @Transactional
    public void revokeOthers(Long userId, String keepToken) {
        if (keepToken == null || keepToken.isBlank()) {
            refreshTokenRepository.deleteByUserId(userId);
        } else {
            refreshTokenRepository.deleteByUserIdExceptToken(userId, keepToken);
        }
    }

    @Transactional
    public void revokeOneSession(Long userId, String token) {
        RefreshToken session = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Session introuvable"));
        if (!session.getUser().getId().equals(userId)) {
            throw new InvalidTokenException("Session introuvable");
        }
        refreshTokenRepository.delete(session);
    }
}
