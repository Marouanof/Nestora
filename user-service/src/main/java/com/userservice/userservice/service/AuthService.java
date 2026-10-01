package com.userservice.userservice.service;

import com.userservice.userservice.dto.*;
import com.userservice.userservice.entity.RefreshToken;
import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.enums.RoleName;
import com.userservice.userservice.exception.*;
import com.userservice.userservice.repository.RoleRepository;
import com.userservice.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setEmailVerified(false);
        if (Boolean.TRUE.equals(request.getAcceptTerms())) {
            user.setTermsAcceptedAt(LocalDateTime.now());
        }

        user.addRole(resolveRole(request.getRole()));

        user.setDescription(request.getDescription());
        user.setDateNaissance(request.getDateNaissance());
        user.setCountry(request.getCountry());
        user.setCity(request.getCity());

        // Générer le token de vérification email
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);
        user.setEmailVerificationToken(hashedToken, 24);

        User savedUser = userRepository.save(user);

        // Email de vérification best-effort : ne jamais faire échouer le register si le SMTP est down
        try {
            sendVerificationEmail(savedUser, rawToken);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class)
                    .warn("Verification email non envoye a {} : {}", savedUser.getEmail(), e.getMessage());
        }

        return new RegisterResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("Compte temporairement verrouillé suite à trop de tentatives échouées. Réessayez plus tard.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            registerFailedLoginAttempt(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new AccountDisabledException("User account is disabled");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email address not verified");
        }

        String token = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), userAgent, ipAddress);

        // Mettre à jour le dernier login et réinitialiser les compteurs d'échec
        user.setLastLogin(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return new AuthResponse(
                token,
                refreshToken.getToken(),
                user
        );
    }

    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        // Réutilisation d'un token déjà révoqué -> attaque potentielle : révoquer toute la famille
        if (token.isRevoked()) {
            refreshTokenService.revokeAllUserTokens(token.getUser().getId());
            throw new InvalidTokenException("Refresh token invalide. Veuillez vous reconnecter.");
        }

        refreshTokenService.verifyExpiration(token);

        User user = token.getUser();
        String newJwtToken = jwtTokenProvider.generateToken(user);
        // Rotation multi-session : révoquer uniquement le token présenté, préserver les autres appareils
        String oldTokenValue = token.getToken();
        refreshTokenService.revokeToken(oldTokenValue);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                user.getId(), token.getUserAgent(), token.getIpAddress());

        return new AuthResponse(
                newJwtToken,
                newRefreshToken.getToken(),
                user
        );
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        // 1. Vérifier si l'ancien mot de passe est correct
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("L'ancien mot de passe est incorrect");
        }

        // 2. Mettre à jour avec le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private void registerFailedLoginAttempt(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }

    @Transactional
    public String verifyEmail(String token) {
        String hashedToken = hashToken(token);
        User user = userRepository.findByEmailVerificationToken(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Token invalide ou expiré"));

        if (user.verifyEmail(hashedToken)) {
            userRepository.save(user);
            return "Email vérifié avec succès";
        } else {
            throw new InvalidTokenException("Token expiré");
        }
    }

    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        if (user.isEmailVerified()) {
            throw new InvalidActionException("Email déjà vérifié");
        }

        // Cooldown persisté en DB (fonctionne en multi-instances, survive aux redémarrages)
        LocalDateTime lastAttempt = user.getLastVerificationEmailSentAt();
        if (lastAttempt != null &&
                lastAttempt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new InvalidActionException("Veuillez attendre avant de demander un nouvel email");
        }

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);
        user.setEmailVerificationToken(hashedToken, 24);
        user.setLastVerificationEmailSentAt(LocalDateTime.now());
        userRepository.save(user);

        sendVerificationEmail(user, rawToken);
    }

    @Transactional
    public void requestEmailChange(Long userId, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        String rawToken = UUID.randomUUID().toString();
        user.setPendingEmail(newEmail);
        user.setPendingEmailToken(hashToken(rawToken));
        user.setPendingEmailExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        sendEmailChangeVerification(newEmail, user.getFullName(), rawToken);
    }

    @Transactional
    public String confirmEmailChange(String token) {
        String hashed = hashToken(token);
        User user = userRepository.findByPendingEmailToken(hashed)
                .orElseThrow(() -> new InvalidTokenException("Token invalide ou expiré"));
        if (user.getPendingEmailExpiry() == null
                || LocalDateTime.now().isAfter(user.getPendingEmailExpiry())) {
            throw new InvalidTokenException("Token expiré");
        }
        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            throw new InvalidTokenException("Aucun changement d'email en attente");
        }
        if (userRepository.existsByEmail(user.getPendingEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setPendingEmailToken(null);
        user.setPendingEmailExpiry(null);
        user.setEmailVerified(true);
        userRepository.save(user);
        return "Email changé et vérifié avec succès";
    }

    private void sendEmailChangeVerification(String toEmail, String fullName, String token) {
        String verificationUrl = verificationBaseUrl + "/verify-email-change?token=" + token;
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(toEmail);
        email.setSubject("Confirmez votre nouvel email");
        email.setText("Bonjour " + fullName + ",\n\n"
                + "Cliquez sur ce lien pour confirmer votre nouvel email: " + verificationUrl + "\n\n"
                + "Ce lien expirera dans 24 heures.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n"
                + "Cordialement,\nL'équipe Real Estate");
        mailSender.send(email);
    }

    private Role resolveRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return roleRepository.findByName(RoleName.ROLE_TENANT)
                    .orElseThrow(() -> new IllegalStateException("ROLE_TENANT introuvable"));
        }
        try {
            RoleName name = RoleName.valueOf(roleName.toUpperCase());
            if (name == RoleName.ROLE_ADMIN) {
                // Bloquer la création de compte admin via l'endpoint public -> TENANT
                return roleRepository.findByName(RoleName.ROLE_TENANT)
                        .orElseThrow(() -> new IllegalStateException("ROLE_TENANT introuvable"));
            }
            return roleRepository.findByName(name)
                    .orElseThrow(() -> new IllegalStateException("Rôle introuvable: " + name));
        } catch (IllegalArgumentException e) {
            // Par défaut ou en cas d'erreur/tentative illégale -> TENANT
            return roleRepository.findByName(RoleName.ROLE_TENANT)
                    .orElseThrow(() -> new IllegalStateException("ROLE_TENANT introuvable"));
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur de hachage du token", e);
        }
    }

    @Transactional
    public void forceLogoutUser(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }

    @org.springframework.beans.factory.annotation.Value("${app.verification-base-url:http://localhost:5173}")
    private String verificationBaseUrl;

    private void sendVerificationEmail(User user, String token) {
        String verificationUrl = verificationBaseUrl + "/verify-email?token=" + token;

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject("Vérification de votre email");
        email.setText("Bonjour " + user.getFullName() + ",\n\n" +
                "Cliquez sur ce lien pour vérifier votre email: " + verificationUrl + "\n\n" +
                "Ce lien expirera dans 24 heures.\n\n" +
                "Cordialement,\nL'équipe Real Estate");

        mailSender.send(email);
    }
}
