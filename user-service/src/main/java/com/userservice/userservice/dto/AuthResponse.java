package com.userservice.userservice.dto;

import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.enums.RoleName;
import lombok.Data;

import java.util.List;

@Data
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<RoleName> roles;
    private boolean emailVerified;

    public AuthResponse(String token, String refreshToken, User user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userId = user.getId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFullName();
        this.roles = user.getRoles().stream().map(Role::getName).toList();
        this.emailVerified = user.isEmailVerified();
    }
}
