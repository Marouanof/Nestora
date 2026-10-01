package com.userservice.userservice.service;

import com.userservice.userservice.dto.CreateUserRequest;
import com.userservice.userservice.dto.UpdateProfileRequest;
import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.enums.RoleName;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.kyc.entity.KycDocument;
import com.userservice.userservice.kyc.entity.KycVerification;
import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import com.userservice.userservice.kyc.service.KycService;
import com.userservice.userservice.repository.RefreshTokenRepository;
import com.userservice.userservice.repository.RoleRepository;
import com.userservice.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KycService kycService;

    @Transactional
    public void createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.userservice.userservice.exception.EmailAlreadyExistsException("Un utilisateur avec cet email existe déjà");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.addRole(resolveRole(request.getRole()));
        userRepository.save(user);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Transactional
    public void enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setEnabled(false);
        userRepository.save(user);
    }

    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                        query, query, query, pageable)
                .map(this::mapToUserResponse);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }
        refreshTokenRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    public Page<UserResponse> searchUsersAdvanced(
            RoleName role,
            String city,
            String country,
            Pageable pageable) {

        Specification<User> spec = Specification.where(null);

        if (role != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("roles").get("name"), role));
        }

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
        }

        if (country != null && !country.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase() + "%"));
        }

        return userRepository.findAll(spec, pageable)
                .map(this::mapToUserResponse);
    }

    @Transactional
    public UserResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

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

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void updateUserRoles(Long userId, List<RoleName> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.getRoles().clear();
        if (newRoles != null) {
            for (RoleName name : newRoles) {
                roleRepository.findByName(name).ifPresent(user.getRoles()::add);
            }
        }
        userRepository.save(user);
    }

    private Role resolveRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return roleRepository.findByName(RoleName.ROLE_TENANT)
                    .orElseThrow(() -> new IllegalStateException("ROLE_TENANT introuvable"));
        }
        try {
            return roleRepository.findByName(RoleName.valueOf(roleName.toUpperCase()))
                    .orElseThrow(() -> new IllegalStateException("Rôle introuvable: " + roleName));
        } catch (IllegalArgumentException e) {
            return roleRepository.findByName(RoleName.ROLE_TENANT)
                    .orElseThrow(() -> new IllegalStateException("ROLE_TENANT introuvable"));
        }
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
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .photoUrl(user.getPhotoUrl())
                .kycRectoUrl(doc != null ? doc.getRectoUrl() : null)
                .kycVersoUrl(doc != null ? doc.getVersoUrl() : null)
                .kycStatus(verification != null ? verification.getStatus().name() : null)
                .kycVerified(verification != null && verification.getStatus() == KycVerificationStatus.VERIFIED)
                .rejectionReason(verification != null ? verification.getRejectionReason() : null)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
