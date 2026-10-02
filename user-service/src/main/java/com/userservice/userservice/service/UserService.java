package com.userservice.userservice.service;

import com.userservice.userservice.client.PropertyServiceClient;
import com.userservice.userservice.dto.CompletenessResponse;
import com.userservice.userservice.dto.PropertyResponseDTO;
import com.userservice.userservice.dto.UpdateProfileRequest;
import com.userservice.userservice.dto.UserFullResponse;
import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.enums.RoleName;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.kyc.entity.KycDocument;
import com.userservice.userservice.kyc.entity.KycVerification;
import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import com.userservice.userservice.kyc.service.KycService;
import com.userservice.userservice.repository.OwnerListingPreferenceRepository;
import com.userservice.userservice.repository.PhoneVerificationRepository;
import com.userservice.userservice.repository.RefreshTokenRepository;
import com.userservice.userservice.repository.RoleRepository;
import com.userservice.userservice.repository.TenantPreferenceRepository;
import com.userservice.userservice.repository.UserRepository;
import com.userservice.userservice.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final KycService kycService;
    private final TenantPreferenceRepository tenantPreferenceRepository;
    private final OwnerListingPreferenceRepository ownerListingPreferenceRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public String updateFileUrl(Long userId, String fileUrl, String type) {
        User user = findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        switch (type.toLowerCase()) {
            case "avatar":
            case "photo":
                user.setPhotoUrl(fileUrl);
                break;
            default:
                // Si c'est un autre type, on peut décider de ne rien faire ou de lever une exception
                log.warn("Unknown file type for DB storage: {}", type);
                break;
        }

        userRepository.save(user);
        return fileUrl;
    }

    public UserResponse getCurrentUserDto(Long userId) {
        return findById(userId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // ✅ Self-service : un Tenant peut devenir Owner
    @Transactional
    public UserResponse becomeOwner(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.hasRole(RoleName.ROLE_OWNER)) {
            Role ownerRole = roleRepository.findByName(RoleName.ROLE_OWNER)
                    .orElseThrow(() -> new IllegalStateException("ROLE_OWNER introuvable"));
            user.addRole(ownerRole);
            userRepository.save(user);
        }
        return mapToResponse(user);
    }

    public UserFullResponse getUserFullResponse(Long userId) {
        UserResponse userResponse = getCurrentUserDto(userId);
        
        List<PropertyResponseDTO> properties = List.of();
        try {
            properties = propertyServiceClient.getPropertiesByOwner(userId);
        } catch (Exception e) {
            log.error("Failed to fetch properties for user {}: {}", userId, e.getMessage());
        }

        return UserFullResponse.builder()
                .userInfo(userResponse)
                .properties(properties)
                .build();
    }

    public List<UserResponse> getUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompletenessResponse getCompleteness(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        KycVerification verification = kycService.getLatestVerification(userId).orElse(null);
        KycDocument doc = kycService.getLatestDocument(userId).orElse(null);

        boolean hasPhoto = user.getPhotoUrl() != null && !user.getPhotoUrl().isBlank();
        boolean hasRecto = doc != null && doc.getRectoUrl() != null && !doc.getRectoUrl().isBlank();
        boolean hasVerso = doc != null && doc.getVersoUrl() != null && !doc.getVersoUrl().isBlank();
        boolean hasSelfie = doc != null && doc.getSelfieUrl() != null && !doc.getSelfieUrl().isBlank();
        boolean kycComplete = verification != null
                && verification.getStatus() == KycVerificationStatus.VERIFIED;
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();
        boolean hasTenantPrefs = tenantPreferenceRepository.findByUserId(userId).isPresent();
        boolean hasDescription = user.getDescription() != null && !user.getDescription().isBlank();

        int percent = 0;
        List<String> missing = new ArrayList<>();
        List<CompletenessResponse.NextAction> actions = new ArrayList<>();

        if (hasPhoto) {
            percent += 20;
        } else {
            missing.add("PHOTO");
            actions.add(new CompletenessResponse.NextAction("UPLOAD_PHOTO",
                    "Ajoutez une photo de profil", "/profile/photo", true));
        }
        if (kycComplete) {
            percent += 40;
        } else {
            if (!hasRecto) {
                missing.add("KYC_RECTO");
            }
            if (!hasVerso) {
                missing.add("KYC_VERSO");
            }
            if (!hasSelfie) {
                missing.add("KYC_SELFIE");
            }
            if (missing.stream().noneMatch(m -> m.startsWith("KYC_"))) {
                // Docs présentes mais pas encore vérifiées
                missing.add("KYC_PENDING");
            }
            actions.add(new CompletenessResponse.NextAction("SUBMIT_KYC",
                    "Vérifiez votre identité", "/owner/verification", true));
        }
        if (hasPhone) {
            percent += 15;
        } else {
            missing.add("PHONE");
            actions.add(new CompletenessResponse.NextAction("ADD_PHONE",
                    "Ajoutez votre téléphone", "/profile", true));
        }
        if (hasTenantPrefs) {
            percent += 15;
        } else {
            missing.add("TENANT_PREFS");
            actions.add(new CompletenessResponse.NextAction("SET_PREFS",
                    "Complétez vos préférences", "/onboarding", false));
        }
        if (hasDescription) {
            percent += 10;
        } else {
            missing.add("DESCRIPTION");
            actions.add(new CompletenessResponse.NextAction("ADD_BIO",
                    "Ajoutez une description", "/profile", false));
        }

        return new CompletenessResponse(percent, kycComplete, user.isEmailVerified(), missing, actions);
    }

    // RGPD : anonymisation du compte (droit à l'effacement) + purge données liées
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Purge données liées avant anonymisation (conserve reviews pour intégrité)
        try {
            kycService.deleteAllForUser(userId);
        } catch (Exception e) {
            log.warn("KYC purge failed for user {}: {}", userId, e.getMessage());
        }
        try {
            wishlistItemRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Wishlist purge failed for user {}: {}", userId, e.getMessage());
        }
        try {
            tenantPreferenceRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Tenant prefs purge failed for user {}: {}", userId, e.getMessage());
        }
        try {
            ownerListingPreferenceRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Owner prefs purge failed for user {}: {}", userId, e.getMessage());
        }
        try {
            refreshTokenRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Refresh tokens purge failed for user {}: {}", userId, e.getMessage());
        }
        try {
            phoneVerificationRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Phone verifications purge failed for user {}: {}", userId, e.getMessage());
        }

        // On conserve la ligne pour l'intégrité référentielle (reviews, etc.)
        // mais toutes les données personnelles sont effacées/anonymisées.
        user.setEmail("deleted+" + java.util.UUID.randomUUID() + "@anonymous.local");
        user.setFirstName("Utilisateur");
        user.setLastName("Supprimé");
        user.setPhone(null);
        user.setPhoneVerified(false);
        user.setPendingEmail(null);
        user.setPendingEmailToken(null);
        user.setPendingEmailExpiry(null);
        user.setPassword(null);
        user.setDescription(null);
        user.setDateNaissance(null);
        user.setCountry(null);
        user.setCity(null);
        user.setPhotoUrl(null);
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(null, 0);
        user.getRoles().clear();

        userRepository.save(user);
        log.info("User {} anonymized and purged (GDPR deletion)", userId);
    }

    private UserResponse mapToResponse(User u) {
        KycVerification verification = kycService.getLatestVerification(u.getId()).orElse(null);
        KycDocument doc = kycService.getLatestDocument(u.getId()).orElse(null);
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .enabled(u.isEnabled())
                .emailVerified(u.isEmailVerified())
                .roles(u.getRoles().stream().map(Role::getName).toList())
                .description(u.getDescription())
                .dateNaissance(u.getDateNaissance())
                .country(u.getCountry())
                .city(u.getCity())
                .accountType(u.getAccountType())
                .phone(u.getPhone())
                .phoneVerified(u.isPhoneVerified())
                .photoUrl(u.getPhotoUrl())
                .kycRectoUrl(doc != null ? doc.getRectoUrl() : null)
                .kycVersoUrl(doc != null ? doc.getVersoUrl() : null)
                .kycSelfieUrl(doc != null ? doc.getSelfieUrl() : null)
                .kycStatus(verification != null ? verification.getStatus().name() : null)
                .kycVerified(verification != null && verification.getStatus() == KycVerificationStatus.VERIFIED)
                .rejectionReason(verification != null ? verification.getRejectionReason() : null)
                .createdAt(u.getCreatedAt())
                .lastLogin(u.getLastLogin())
                .build();
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Vérifier si le téléphone est déjà utilisé
        if (request.phone() != null && !request.phone().isBlank()) {
            Optional<User> existingUser = userRepository.findByPhone(request.phone());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new com.userservice.userservice.exception.InvalidActionException("Ce numéro de téléphone est déjà utilisé");
            }
        }

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            if (!request.phone().equals(user.getPhone())) {
                user.setPhone(request.phone());
                user.setPhoneVerified(false);
            }
        }
        if (request.description() != null) {
            user.setDescription(request.description());
        }
        if (request.dateNaissance() != null) {
            user.setDateNaissance(request.dateNaissance());
        }
        if (request.country() != null && !request.country().isBlank()) {
            user.setCountry(request.country());
        }
        if (request.city() != null && !request.city().isBlank()) {
            user.setCity(request.city());
        }
        if (request.accountType() != null) {
            user.setAccountType(request.accountType());
        }

        User saved = userRepository.save(user);
        return mapToUserResponse(saved);
    }

    private UserResponse mapToUserResponse(User user) {
        KycVerification verification = kycService.getLatestVerification(user.getId()).orElse(null);
        KycDocument doc = kycService.getLatestDocument(user.getId()).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .description(user.getDescription())
                .dateNaissance(user.getDateNaissance())
                .country(user.getCountry())
                .city(user.getCity())
                .accountType(user.getAccountType())
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .photoUrl(user.getPhotoUrl())
                .kycRectoUrl(doc != null ? doc.getRectoUrl() : null)
                .kycVersoUrl(doc != null ? doc.getVersoUrl() : null)
                .kycSelfieUrl(doc != null ? doc.getSelfieUrl() : null)
                .kycStatus(verification != null ? verification.getStatus().name() : null)
                .kycVerified(verification != null && verification.getStatus() == KycVerificationStatus.VERIFIED)
                .rejectionReason(verification != null ? verification.getRejectionReason() : null)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
