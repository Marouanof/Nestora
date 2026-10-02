package com.userservice.userservice.dto;

import com.userservice.userservice.enums.RoleName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean emailVerified;
    private List<RoleName> roles;
    private String description;
    private LocalDate dateNaissance;
    private String country;
    private String city;
    private com.userservice.userservice.enums.AccountType accountType;
    private String phone;
    private boolean phoneVerified;
    private String photoUrl;
    private String kycRectoUrl;
    private String kycVersoUrl;
    private String kycSelfieUrl;
    private String kycStatus;
    private boolean kycVerified;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
