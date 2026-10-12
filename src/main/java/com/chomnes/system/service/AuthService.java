package com.chomnes.system.service;

import com.chomnes.system.dto.BootstrapAdminRequest;
import com.chomnes.system.dto.ChangePasswordRequest;
import com.chomnes.system.dto.CreateUserRequest;
import com.chomnes.system.dto.LoginRequest;
import com.chomnes.system.dto.LoginResponse;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.exception.AdminBootstrapCompleteException;
import com.chomnes.system.exception.InvalidCredentialsException;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AuthService {
    private static final String TOKEN_ISSUER = "chomnes-system";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final UserService userService;
    private final long expirationMillis;
    private final String bootstrapKey;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            UserService userService,
            @Value("${app.jwt.expiration-ms:900000}") long expirationMillis,
            @Value("${app.auth.bootstrap-key:}") String bootstrapKey
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.userService = userService;
        this.bootstrapKey = bootstrapKey;
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT expiration must be a positive number of milliseconds.");
        }
        if (!bootstrapKey.isBlank() && bootstrapKey.length() < 32) {
            throw new IllegalArgumentException("ADMIN_BOOTSTRAP_KEY must be at least 32 characters when configured.");
        }
        this.expirationMillis = expirationMillis;
    }

    public synchronized UserResponse bootstrapAdmin(BootstrapAdminRequest request) {
        if (bootstrapKey.isBlank() || !MessageDigest.isEqual(
                bootstrapKey.getBytes(StandardCharsets.UTF_8),
                request.setupKey().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new AccessDeniedException("Administrator setup is not authorized.");
        }
        if (userRepository.existsByRole(User.Role.Admin)) {
            throw new AdminBootstrapCompleteException();
        }
        return userService.create(new CreateUserRequest(
                request.username(),
                request.email(),
                request.password(),
                User.Role.Admin
        ));
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(account -> account.getStatus() == User.Status.Active)
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expirationMillis);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(TOKEN_ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims
        )).getTokenValue();

        user.setLastLoginAt(LocalDateTime.ofInstant(issuedAt, ZoneOffset.UTC));
        userRepository.save(user);

        return new LoginResponse(
                token,
                "Bearer",
                expirationMillis / 1000,
                UserResponse.from(user)
        );
    }

    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .filter(account -> account.getStatus() == User.Status.Active)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 bytes when encoded as UTF-8.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
