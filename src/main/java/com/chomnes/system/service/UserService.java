package com.chomnes.system.service;

import com.chomnes.system.dto.CreateUserRequest;
import com.chomnes.system.dto.UpdateUserRequest;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.exception.DuplicateUserException;
import com.chomnes.system.exception.UserNotFoundException;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "userId"))
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer userId, Integer requesterId, boolean administrator) {
        User user = getUser(userId);
        requireOwnerOrAdministrator(userId, requesterId, administrator);
        return UserResponse.from(user);
    }

    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        ensureUnique(username, email, null);

        User user = new User(
                username,
                email,
                hashPassword(request.password()),
                request.role()
        );
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(Integer userId, UpdateUserRequest request, Integer requesterId, boolean administrator) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one user field to update.");
        }

        User user = getUser(userId);
        requireOwnerOrAdministrator(userId, requesterId, administrator);
        if (!administrator && (request.role() != null || request.status() != null)) {
            throw new AccessDeniedException("Only an administrator can change a user's role or status.");
        }
        if (requesterId.equals(userId) && (request.role() != null || request.status() != null)) {
            throw new AccessDeniedException("You cannot change your own role or status.");
        }
        if (requesterId.equals(userId) && request.password() != null) {
            throw new AccessDeniedException("Use the authenticated password-change endpoint.");
        }

        String username = request.username() == null ? null : request.username().trim();
        String email = request.email() == null ? null : normalizeEmail(request.email());

        if (username != null && username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        if (email != null && email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        ensureUnique(username, email, userId);

        if (request.status() == User.Status.Inactive
                && (user.getRole() == User.Role.Admin || request.role() == User.Role.Admin)) {
            throw new AccessDeniedException("An administrator account cannot be deactivated.");
        }

        if (username != null) user.setUsername(username);
        if (email != null) user.setEmail(email);
        if (request.password() != null) user.setPasswordHash(hashPassword(request.password()));
        if (request.role() != null) user.setRole(request.role());
        if (request.status() != null) user.setStatus(request.status());

        return UserResponse.from(userRepository.save(user));
    }

    public void deactivate(Integer userId, Integer requesterId) {
        User user = getUser(userId);
        if (requesterId.equals(userId)) {
            throw new AccessDeniedException("You cannot deactivate your own account.");
        }
        if (user.getRole() == User.Role.Admin) {
            throw new AccessDeniedException("An administrator account cannot be deactivated.");
        }
        user.setStatus(User.Status.Inactive);
        userRepository.save(user);
    }

    private void requireOwnerOrAdministrator(Integer userId, Integer requesterId, boolean administrator) {
        if (!administrator && !requesterId.equals(userId)) {
            throw new AccessDeniedException("You can only access your own user account.");
        }
    }

    private User getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void ensureUnique(String username, String email, Integer userId) {
        if (username != null && (userId == null
                ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndUserIdNot(username, userId))) {
            throw new DuplicateUserException("username");
        }
        if (email != null && (userId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndUserIdNot(email, userId))) {
            throw new DuplicateUserException("email");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hashPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 bytes when encoded as UTF-8.");
        }
        return passwordEncoder.encode(password);
    }
}
