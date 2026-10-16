package com.chomnes.system.service;

import com.chomnes.system.dto.CreateUserRequest;
import com.chomnes.system.dto.UpdateUserRequest;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.exception.DuplicateUserException;
import com.chomnes.system.exception.UserNotFoundException;
import com.chomnes.system.model.User;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import com.chomnes.system.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        StudentRepository studentRepository,
        TeacherRepository teacherRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
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
        return create(request, false);
    }

    public UserResponse create(CreateUserRequest request, boolean createRoleProfile) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        ensureUnique(username, email, null);

        if (createRoleProfile && (request.role() == User.Role.Student || request.role() == User.Role.Teacher)
            && (request.firstName() == null || request.firstName().isBlank()
            || request.lastName() == null || request.lastName().isBlank())) {
            throw new IllegalArgumentException("First name and last name are required for student and teacher accounts.");
        }

        User user = new User(
            username,
            email,
            hashPassword(request.password()),
            request.role()
        );
        User savedUser = userRepository.save(user);
        if (createRoleProfile && request.role() == User.Role.Student) {
            studentRepository.save(new Student(
                savedUser.getUserId(), request.firstName().trim(), request.lastName().trim(), request.dob(),
                request.gender(), request.photoUrl(), request.contactNumber(), request.address(),
                request.enrollmentDate() == null ? LocalDate.now() : request.enrollmentDate()
            ));
        } else if (createRoleProfile && request.role() == User.Role.Teacher) {
            teacherRepository.save(new Teacher(
                savedUser.getUserId(), request.firstName().trim(), request.lastName().trim(), request.dob(),
                request.gender(), request.photoUrl(), request.contactNumber(), request.specialization(),
                request.bio(), LocalDate.now()
            ));
        }
        return UserResponse.from(savedUser);
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

    public UserResponse deactivate(Integer userId, Integer requesterId) {
        User user = getUser(userId);
        if (requesterId.equals(userId)) {
            throw new AccessDeniedException("You cannot deactivate your own account.");
        }
        if (user.getRole() == User.Role.Admin) {
            throw new AccessDeniedException("An administrator account cannot be deactivated.");
        }
        if (user.getStatus() == User.Status.Inactive) {
            throw new IllegalArgumentException("User is already deactivated.");
        }
        user.setStatus(User.Status.Inactive);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse activate(Integer userId) {
        User user = getUser(userId);
        if (user.getStatus() == User.Status.Active) {
            throw new IllegalArgumentException("User is already active.");
        }
        user.setStatus(User.Status.Active);
        return UserResponse.from(userRepository.save(user));
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
