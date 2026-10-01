package com.userservice.userservice.dto;

import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.enums.RoleName;
import lombok.Data;

import java.util.List;

@Data
public class RegisterResponse {
    private Long userId;
    private String email;
    private String fullName;
    private List<RoleName> roles;
    private boolean emailVerified;

    public RegisterResponse(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.roles = user.getRoles().stream().map(Role::getName).toList();
        this.emailVerified = user.isEmailVerified();
    }
}
