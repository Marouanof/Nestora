package com.userservice.userservice.service;

import com.userservice.userservice.client.PropertyServiceClient;
import com.userservice.userservice.dto.PropertyResponseDTO;
import com.userservice.userservice.dto.UpdateProfileRequest;
import com.userservice.userservice.dto.UserFullResponse;
import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.entity.KycDocument;
import com.userservice.userservice.entity.KycStatus;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final KycDocumentService kycDocumentService;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(java.time.LocalDateTime.now());
            userRepository.save(user);
        });
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

    private UserResponse mapToResponse(User u) {
        KycDocument kyc = kycDocumentService.getLatestKyc(u.getId()).orElse(null);
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .enabled(u.isEnabled())
                .emailVerified(u.isEmailVerified())
                .role(u.getRole())
                .description(u.getDescription())
                .dateNaissance(u.getDateNaissance())
                .country(u.getCountry())
                .city(u.getCity())
                .phone(u.getPhone())
                .photoUrl(u.getPhotoUrl())
                .kycRectoUrl(kyc != null ? kyc.getRectoUrl() : null)
                .kycVersoUrl(kyc != null ? kyc.getVersoUrl() : null)
                .kycStatus(kyc != null ? kyc.getStatus().name() : null)
                .kycVerified(kyc != null && kyc.getStatus() == KycStatus.APPROVED)
                .rejectionReason(kyc != null ? kyc.getRejectionReason() : null)
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
            user.setPhone(request.phone());
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

        User saved = userRepository.save(user);
        return mapToUserResponse(saved);
    }

    private UserResponse mapToUserResponse(User user) {
        KycDocument kyc = kycDocumentService.getLatestKyc(user.getId()).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .role(user.getRole())
                .description(user.getDescription())
                .dateNaissance(user.getDateNaissance())
                .country(user.getCountry())
                .city(user.getCity())
                .phone(user.getPhone())
                .photoUrl(user.getPhotoUrl())
                .kycRectoUrl(kyc != null ? kyc.getRectoUrl() : null)
                .kycVersoUrl(kyc != null ? kyc.getVersoUrl() : null)
                .kycStatus(kyc != null ? kyc.getStatus().name() : null)
                .kycVerified(kyc != null && kyc.getStatus() == KycStatus.APPROVED)
                .rejectionReason(kyc != null ? kyc.getRejectionReason() : null)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
