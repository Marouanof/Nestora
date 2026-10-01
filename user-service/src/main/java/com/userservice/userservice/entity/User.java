package com.userservice.userservice.entity;

import com.userservice.userservice.enums.AccountType;
import com.userservice.userservice.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "phone")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = true)
    private String password; // Hashé avec bcrypt

    @Column(length = 500)
    private String description; // ✅ Nouveau

    @Column(name = "date_naissance")
    private LocalDate dateNaissance; // ✅ Nouveau - LocalDate au lieu de String

    private String country; // ✅ Nouveau
    private String city; // ✅ Nouveau

    @Column(name = "account_type")
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    // Email verification fields (au lieu de table séparée)
    @Column(name = "email_verification_token")
    private String emailVerificationToken; // ✅ Nouveau - token hashé

    @Column(name = "email_token_expiry")
    private LocalDateTime emailTokenExpiry; // ✅ Nouveau

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(name = "pending_email_token")
    private String pendingEmailToken;

    @Column(name = "pending_email_expiry")
    private LocalDateTime pendingEmailExpiry;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    // Many-to-many roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_verification_email_sent_at")
    private LocalDateTime lastVerificationEmailSentAt;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .toList();
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    // ✅ Méthode pour vérifier si le token email est valide
    public boolean isEmailTokenValid() {
        return emailVerificationToken != null &&
                emailTokenExpiry != null &&
                LocalDateTime.now().isBefore(emailTokenExpiry);
    }

    // ✅ Méthode pour mettre à jour le token de vérification
    public void setEmailVerificationToken(String token, int expiryHours) {
        this.emailVerificationToken = token;
        this.emailTokenExpiry = LocalDateTime.now().plusHours(expiryHours);
        this.emailVerified = false;
    }

    // ✅ Méthode pour vérifier l'email
    public boolean verifyEmail(String token) {
        if (emailVerificationToken != null &&
                emailVerificationToken.equals(token) &&
                isEmailTokenValid()) {
            this.emailVerified = true;
            this.emailVerificationToken = null;
            this.emailTokenExpiry = null;
            return true;
        }
        return false;
    }
}
