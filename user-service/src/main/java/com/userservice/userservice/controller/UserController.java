package com.userservice.userservice.controller;

import com.userservice.userservice.dto.ChangeEmailRequest;
import com.userservice.userservice.dto.CompletenessResponse;
import com.userservice.userservice.dto.KycStatusResponse;
import com.userservice.userservice.dto.OwnerListingPreferenceRequest;
import com.userservice.userservice.dto.OwnerListingPreferenceResponse;
import com.userservice.userservice.dto.TenantPreferenceRequest;
import com.userservice.userservice.dto.TenantPreferenceResponse;
import com.userservice.userservice.dto.UpdateProfileRequest;
import com.userservice.userservice.dto.UserFullResponse;
import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.dto.VerifyCodeRequest;
import com.userservice.userservice.service.AuthService;
import com.userservice.userservice.service.OwnerListingPreferenceService;
import com.userservice.userservice.service.PhoneVerificationService;
import com.userservice.userservice.service.TenantPreferenceService;
import com.userservice.userservice.service.UserReviewService;
import com.userservice.userservice.service.UserService;
import com.userservice.userservice.kyc.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TenantPreferenceService tenantPreferenceService;
    private final OwnerListingPreferenceService ownerListingPreferenceService;
    private final KycService kycService;
    private final UserReviewService userReviewService;
    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;
    private final com.userservice.userservice.service.RefreshTokenService refreshTokenService;

    // Récupère l'utilisateur connecté via header X-Auth-User-Id
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        UserResponse response = userService.getCurrentUserDto(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/full")
    public ResponseEntity<UserFullResponse> getMyFullInfo(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        UserFullResponse response = userService.getUserFullResponse(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @Valid @RequestBody UpdateProfileRequest req) {

        Long userId = Long.valueOf(userIdHeader);
        UserResponse response = userService.updateProfile(userId, req);
        return ResponseEntity.ok(response);
    }

    // RGPD : droit à l'effacement
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    // ✅ Onboarding : préférences de recherche du tenant
    @GetMapping("/me/preferences")
    public ResponseEntity<TenantPreferenceResponse> getMyPreferences(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        TenantPreferenceResponse response = tenantPreferenceService.getPreferences(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<TenantPreferenceResponse> saveMyPreferences(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @Valid @RequestBody TenantPreferenceRequest request) {

        Long userId = Long.valueOf(userIdHeader);
        TenantPreferenceResponse response = tenantPreferenceService.savePreferences(userId, request);
        return ResponseEntity.ok(response);
    }

    // ✅ Onboarding Owner : préférences de listing
    @GetMapping("/me/listing-preferences")
    public ResponseEntity<OwnerListingPreferenceResponse> getMyListingPreferences(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        OwnerListingPreferenceResponse response = ownerListingPreferenceService.getPreferences(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/listing-preferences")
    public ResponseEntity<OwnerListingPreferenceResponse> saveMyListingPreferences(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @Valid @RequestBody OwnerListingPreferenceRequest request) {

        Long userId = Long.valueOf(userIdHeader);
        OwnerListingPreferenceResponse response = ownerListingPreferenceService.savePreferences(userId, request);
        return ResponseEntity.ok(response);
    }

    // ✅ Self-service : devenir Owner
    @PostMapping("/me/become-owner")
    public ResponseEntity<UserResponse> becomeOwner(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        UserResponse response = userService.becomeOwner(userId);
        return ResponseEntity.ok(response);
    }

    // Supprimer une review que j'ai écrite
    @DeleteMapping("/me/reviews/{reviewId}")
    public ResponseEntity<Void> deleteMyReview(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @PathVariable Long reviewId) {

        userReviewService.deleteOwnReview(reviewId, Long.valueOf(userIdHeader));
        return ResponseEntity.noContent().build();
    }

    // ✅ Onboarding Owner : démarrer la vérification d'identité (KYC)
    @PostMapping("/me/kyc/start")
    public ResponseEntity<String> startKycVerification(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        kycService.startVerification(userId);
        return ResponseEntity.ok("Vérification d'identité démarrée");
    }

    @GetMapping("/me/completeness")
    public ResponseEntity<CompletenessResponse> getCompleteness(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {
        return ResponseEntity.ok(userService.getCompleteness(Long.valueOf(userIdHeader)));
    }

    @GetMapping("/me/kyc/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {
        return ResponseEntity.ok(kycService.getStatusDto(Long.valueOf(userIdHeader)));
    }

    @PostMapping("/me/kyc/retry")
    public ResponseEntity<String> retryKyc(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {
        kycService.retry(Long.valueOf(userIdHeader));
        return ResponseEntity.ok("Nouveau cycle de vérification créé");
    }

    @PostMapping("/me/email/change")
    public ResponseEntity<String> requestEmailChange(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @Valid @RequestBody ChangeEmailRequest request) {
        authService.requestEmailChange(Long.valueOf(userIdHeader), request.newEmail());
        return ResponseEntity.accepted().body("Email de confirmation envoyé au nouvel email");
    }

    @PostMapping("/me/email/verify-change")
    public ResponseEntity<String> confirmEmailChange(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(authService.confirmEmailChange(request.token()));
    }

    @PostMapping("/me/phone/otp")
    public ResponseEntity<String> sendPhoneOtp(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @Valid @RequestBody com.userservice.userservice.dto.SendOtpRequest request) {
        phoneVerificationService.sendOtp(Long.valueOf(userIdHeader), request.phone());
        return ResponseEntity.accepted().body("Code OTP envoyé");
    }

    @PostMapping("/me/phone/verify")
    public ResponseEntity<String> verifyPhone(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @Valid @RequestBody com.userservice.userservice.dto.VerifyOtpRequest request) {
        phoneVerificationService.verify(Long.valueOf(userIdHeader), request.code());
        return ResponseEntity.ok("Téléphone vérifié avec succès");
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<com.userservice.userservice.dto.SessionResponse>> listSessions(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Current-Refresh-Token", required = false) String currentToken) {
        Long userId = Long.valueOf(userIdHeader);
        return ResponseEntity.ok(
                refreshTokenService.listSessions(userId, currentToken));
    }

    @DeleteMapping("/me/sessions/others")
    public ResponseEntity<String> revokeOtherSessions(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @RequestBody(required = false) com.userservice.userservice.dto.RevokeOthersRequest request) {
        Long userId = Long.valueOf(userIdHeader);
        String keep = request != null ? request.keepToken() : null;
        refreshTokenService.revokeOthers(userId, keep);
        return ResponseEntity.ok("Autres sessions révoquées");
    }

    @DeleteMapping("/me/sessions/{token}")
    public ResponseEntity<Void> revokeOneSession(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @PathVariable String token) {
        refreshTokenService.revokeOneSession(Long.valueOf(userIdHeader), token);
        return ResponseEntity.noContent().build();
    }

    // ✅ NOUVELLE ENDPOINT : Obtenir les infos d'un utilisateur par ID (pour admin ou autres services)
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        UserResponse response = userService.getCurrentUserDto(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/full")
    public ResponseEntity<UserFullResponse> getUserFullById(@PathVariable Long userId) {
        UserFullResponse response = userService.getUserFullResponse(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserResponse>> getUsersByIds(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(userService.getUsersByIds(userIds));
    }

    // RGPD : portabilité des données
    @GetMapping("/me/export")
    public ResponseEntity<java.util.Map<String, Object>> exportMyData(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        java.util.Map<String, Object> export = new java.util.LinkedHashMap<>();
        export.put("profile", userService.getCurrentUserDto(userId));
        export.put("tenantPreferences", tenantPreferenceService.getPreferences(userId));
        export.put("ownerListingPreferences", ownerListingPreferenceService.getPreferences(userId));

        com.userservice.userservice.kyc.entity.KycVerification kyc =
                kycService.getLatestVerification(userId).orElse(null);
        if (kyc != null) {
            java.util.Map<String, Object> kycData = new java.util.LinkedHashMap<>();
            kycData.put("status", kyc.getStatus());
            kycData.put("startedAt", kyc.getStartedAt());
            kycData.put("verifiedAt", kyc.getVerifiedAt());
            export.put("kyc", kycData);
        }

        return ResponseEntity.ok(export);
    }
}


