package com.userservice.userservice.controller;

import com.userservice.userservice.dto.CreateUserRequest;
import com.userservice.userservice.dto.UpdateProfileRequest;
import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.enums.RoleName;
import com.userservice.userservice.exception.InvalidActionException;
import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import com.userservice.userservice.kyc.service.KycService;
import com.userservice.userservice.service.AdminService;
import com.userservice.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService; // Ajouté pour la méthode forceLogoutUser
    private final KycService kycService;

    /** Un admin ne peut pas se désactiver / supprimer / déconnecter / changer ses propres rôles. */
    private void rejectSelfAction(String callerIdHeader, Long targetId, String action) {
        if (callerIdHeader == null) {
            return;
        }
        try {
            if (Long.valueOf(callerIdHeader).equals(targetId)) {
                throw new InvalidActionException(
                        "Vous ne pouvez pas " + action + " votre propre compte");
            }
        } catch (NumberFormatException e) {
            throw new InvalidActionException("Identifiant appelant invalide");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) KycVerificationStatus kycStatus
    ) {
        Pageable pageable = PageRequest.of(page, size);

        if (role != null || city != null || country != null || kycStatus != null) {
            return ResponseEntity.ok(adminService.searchUsersAdvanced(role, city, country, kycStatus, pageable));
        }

        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody @Valid CreateUserRequest request) {
        adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur créé avec succès");
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return ResponseEntity.ok("Utilisateur activé avec succès");
    }

    @PutMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-User-Id", required = false) String callerIdHeader) {
        rejectSelfAction(callerIdHeader, id, "désactiver");
        adminService.disableUser(id);
        return ResponseEntity.ok("Utilisateur désactivé avec succès");
    }

    @GetMapping("/users/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.searchUsers(query, pageable));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-User-Id", required = false) String callerIdHeader) {
        rejectSelfAction(callerIdHeader, id, "supprimer");
        adminService.deleteUser(id);
        return ResponseEntity.ok("Utilisateur supprimé avec succès");
    }

    @PostMapping("/users/{id}/force-logout")
    public ResponseEntity<?> forceLogoutUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-User-Id", required = false) String callerIdHeader) {
        rejectSelfAction(callerIdHeader, id, "déconnecter");
        authService.forceLogoutUser(id);
        return ResponseEntity.ok("Utilisateur déconnecté avec succès par l'admin");
    }

    @PutMapping("/users/{id}/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProfileRequest request) {
        UserResponse updatedUser = adminService.updateUserProfile(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long id,
            @RequestParam List<RoleName> roles,
            @RequestHeader(value = "X-Auth-User-Id", required = false) String callerIdHeader) {
        rejectSelfAction(callerIdHeader, id, "modifier les rôles de");
        adminService.updateUserRoles(id, roles);
        return ResponseEntity.ok("Rôles mis à jour avec succès");
    }

    @PostMapping("/users/{id}/kyc/approve")
    public ResponseEntity<?> approveKyc(@PathVariable Long id) {
        kycService.approve(id);
        return ResponseEntity.ok("KYC approuvé avec succès");
    }

    @PostMapping("/users/{id}/kyc/reject")
    public ResponseEntity<?> rejectKyc(
            @PathVariable Long id,
            @RequestParam String reason) {
        kycService.reject(id, reason);
        return ResponseEntity.ok("KYC rejeté avec succès");
    }
}