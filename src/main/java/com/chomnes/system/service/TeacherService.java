package com.chomnes.system.service;

import com.chomnes.system.dto.CreateTeacherRequest;
import com.chomnes.system.dto.TeacherResponse;
import com.chomnes.system.dto.UpdateTeacherRequest;
import com.chomnes.system.exception.TeacherConflictException;
import com.chomnes.system.exception.TeacherNotFoundException;
import com.chomnes.system.exception.UserNotFoundException;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.TeacherRepository;
import com.chomnes.system.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public TeacherService(TeacherRepository teacherRepository, UserRepository userRepository) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll() {
        return teacherRepository.findAll(Sort.by(Sort.Direction.ASC, "teacherId"))
                .stream()
                .map(TeacherResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Integer teacherId, Integer requesterId, User.Role requesterRole) {
        Teacher teacher = getTeacher(teacherId);
        if (requesterRole != User.Role.Admin
                && (requesterRole != User.Role.Teacher || !teacher.getUserId().equals(requesterId))) {
            throw new AccessDeniedException("You can only access your own teacher profile.");
        }
        if (requesterRole == User.Role.Teacher && "Inactive".equals(teacher.getStatus())) {
            throw new AccessDeniedException("This teacher profile is inactive.");
        }
        return TeacherResponse.from(teacher);
    }

    public TeacherResponse create(CreateTeacherRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        if (user.getRole() != User.Role.Teacher) {
            throw new IllegalArgumentException("The linked user must have the Teacher role.");
        }
        if (user.getStatus() != User.Status.Active) {
            throw new TeacherConflictException("An inactive user cannot receive a teacher profile.");
        }
        if (teacherRepository.existsByUserId(user.getUserId())) {
            throw new TeacherConflictException("This user already has a teacher profile.");
        }

        Teacher teacher = new Teacher(
                user.getUserId(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.dob(),
                request.gender(),
                request.photoUrl(),
                request.contactNumber(),
                request.specialization(),
                request.bio(),
                request.hireDate()
        );
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public TeacherResponse update(
            Integer teacherId,
            UpdateTeacherRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one teacher field to update.");
        }
        Teacher teacher = getTeacher(teacherId);
        boolean administrator = requesterRole == User.Role.Admin;
        boolean teacherOwner = requesterRole == User.Role.Teacher
                && teacher.getUserId().equals(requesterId);
        if (!administrator && !teacherOwner) {
            throw new AccessDeniedException("Only an administrator or the teacher can update this profile.");
        }
        if (!administrator && (request.hireDate() != null || request.status() != null)) {
            throw new AccessDeniedException("Teachers cannot change hire date or profile status.");
        }
        if (teacherOwner && "Inactive".equals(teacher.getStatus())) {
            throw new AccessDeniedException("This teacher profile is inactive.");
        }

        if (request.firstName() != null) teacher.setFirstName(requireNonBlank(request.firstName(), "First name"));
        if (request.lastName() != null) teacher.setLastName(requireNonBlank(request.lastName(), "Last name"));
        if (request.dob() != null) teacher.setDob(request.dob());
        if (request.gender() != null) teacher.setGender(request.gender());
        if (request.photoUrl() != null) teacher.setPhotoUrl(request.photoUrl());
        if (request.contactNumber() != null) teacher.setContactNumber(request.contactNumber());
        if (request.specialization() != null) teacher.setSpecialization(request.specialization());
        if (request.bio() != null) teacher.setBio(request.bio());
        if (request.hireDate() != null) teacher.setHireDate(request.hireDate());
        if (request.status() != null) {
            teacher.setStatus(request.status());
            User user = getLinkedUser(teacher.getUserId());
            user.setStatus("Active".equals(request.status()) ? User.Status.Active : User.Status.Inactive);
        }
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public void deactivate(Integer teacherId) {
        Teacher teacher = getTeacher(teacherId);
        teacher.setStatus("Inactive");
        teacherRepository.save(teacher);
        User user = getLinkedUser(teacher.getUserId());
        user.setStatus(User.Status.Inactive);
    }

    private Teacher getTeacher(Integer teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
    }

    private User getLinkedUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private String requireNonBlank(String value, String field) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return trimmed;
    }
}
