package com.userservice.userservice.service;

import com.userservice.userservice.entity.PhoneVerification;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.InvalidActionException;
import com.userservice.userservice.exception.InvalidTokenException;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.repository.PhoneVerificationRepository;
import com.userservice.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {

    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_SEND_PER_15MIN = 3;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final PhoneVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void sendOtp(Long userId, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        String normalized = phone == null ? "" : phone.trim();
        if (normalized.isBlank()) {
            throw new InvalidActionException("Numéro de téléphone requis");
        }
        // Unicité téléphone (hors soi-même)
        userRepository.findByPhone(normalized).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new InvalidActionException("Ce numéro de téléphone est déjà utilisé");
            }
        });
        // Rate-limit : 3 envois / 15 min
        long recent = verificationRepository
                .findByUserIdAndCreatedAtAfter(userId, LocalDateTime.now().minusMinutes(15)).size();
        if (recent >= MAX_SEND_PER_15MIN) {
            throw new InvalidActionException("Trop d'envois, réessayez dans 15 minutes");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        PhoneVerification verification = PhoneVerification.builder()
                .userId(userId)
                .phone(normalized)
                .codeHash(hash(code))
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .attempts(0)
                .build();
        verificationRepository.save(verification);
        // TODO: brancher SMS provider (Twilio/Vonage). En dev on loggue.
        log.info("OTP for user {} phone {} : {}", userId, normalized, code);
    }

    @Transactional
    public void verify(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        PhoneVerification verification = verificationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new InvalidTokenException("Aucun code demandé"));
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            throw new InvalidTokenException("Code expiré");
        }
        if (verification.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            throw new InvalidActionException("Trop de tentatives, redemandez un code");
        }
        verification.setAttempts(verification.getAttempts() + 1);
        verificationRepository.save(verification);
        if (!MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.UTF_8),
                verification.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidTokenException("Code invalide");
        }
        user.setPhone(verification.getPhone());
        user.setPhoneVerified(true);
        userRepository.save(user);
        verificationRepository.deleteByUserId(userId);
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de hachage OTP", e);
        }
    }
}
