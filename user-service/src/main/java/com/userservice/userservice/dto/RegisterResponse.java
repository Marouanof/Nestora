package com.userservice.userservice.dto;

import com.userservice.userservice.entity.User;
import com.userservice.userservice.enu.RoleName;
import lombok.Data;

@Data
public class RegisterResponse {
    private Long userId;
    private String email;
    private String fullName;
    private RoleName role;
    private boolean emailVerified;

    public RegisterResponse(Long userId, String email, String fullName) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }

    public RegisterResponse(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.emailVerified = user.isEmailVerified();
    }
}
