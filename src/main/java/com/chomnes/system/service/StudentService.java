package com.chomnes.system.service;

import com.chomnes.system.dto.CreateStudentRequest;
import com.chomnes.system.dto.StudentResponse;
import com.chomnes.system.dto.UpdateStudentRequest;
import com.chomnes.system.exception.StudentConflictException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.exception.UserNotFoundException;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class StudentService {
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "studentId"))
                .stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Integer studentId, Integer requesterId, User.Role requesterRole) {
        Student student = getStudent(studentId);
        if (requesterRole == User.Role.Student && !student.getUserId().equals(requesterId)) {
            throw new AccessDeniedException("Students can only access their own profile.");
        }
        if (requesterRole == User.Role.Student && "Inactive".equals(student.getStatus())) {
            throw new AccessDeniedException("This student profile is inactive.");
        }
        if (requesterRole != User.Role.Student
                && requesterRole != User.Role.Teacher
                && requesterRole != User.Role.Admin) {
            throw new AccessDeniedException("You cannot access student profiles.");
        }
        return StudentResponse.from(student);
    }

    public StudentResponse create(CreateStudentRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        if (user.getRole() != User.Role.Student) {
            throw new IllegalArgumentException("The linked user must have the Student role.");
        }
        if (user.getStatus() != User.Status.Active) {
            throw new StudentConflictException("An inactive user cannot receive a student profile.");
        }
        if (studentRepository.existsByUserId(user.getUserId())) {
            throw new StudentConflictException("This user already has a student profile.");
        }

        Student student = new Student(
                user.getUserId(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.dob(),
                request.gender(),
                request.photoUrl(),
                request.contactNumber(),
                request.address(),
                request.enrollmentDate() == null ? LocalDate.now() : request.enrollmentDate()
        );
        return StudentResponse.from(studentRepository.save(student));
    }

    public StudentResponse update(
            Integer studentId,
            UpdateStudentRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one student field to update.");
        }
        Student student = getStudent(studentId);
        boolean administrator = requesterRole == User.Role.Admin;
        boolean studentOwner = requesterRole == User.Role.Student
                && student.getUserId().equals(requesterId);
        if (!administrator && !studentOwner) {
            throw new AccessDeniedException("Only an administrator or the student can update this profile.");
        }
        if (!administrator && (request.enrollmentDate() != null || request.status() != null)) {
            throw new AccessDeniedException("Students cannot change enrollment date or profile status.");
        }
        if (studentOwner && "Inactive".equals(student.getStatus())) {
            throw new AccessDeniedException("This student profile is inactive.");
        }

        if (request.firstName() != null) {
            student.setFirstName(requireNonBlank(request.firstName(), "First name"));
        }
        if (request.lastName() != null) {
            student.setLastName(requireNonBlank(request.lastName(), "Last name"));
        }
        if (request.dob() != null) student.setDob(request.dob());
        if (request.gender() != null) student.setGender(request.gender());
        if (request.photoUrl() != null) student.setPhotoUrl(request.photoUrl());
        if (request.contactNumber() != null) student.setContactNumber(request.contactNumber());
        if (request.address() != null) student.setAddress(request.address());
        if (request.enrollmentDate() != null) student.setEnrollmentDate(request.enrollmentDate());
        if (request.status() != null) student.setStatus(request.status());

        studentRepository.save(student);
        if (request.status() != null && "Inactive".equals(request.status())) {
            User user = userRepository.findById(student.getUserId())
                    .orElseThrow(() -> new UserNotFoundException(student.getUserId()));
            user.setStatus(User.Status.Inactive);
            userRepository.save(user);
        }
        return StudentResponse.from(student);
    }

    public void deactivate(Integer studentId) {
        Student student = getStudent(studentId);
        student.setStatus("Inactive");
        studentRepository.save(student);
        User user = userRepository.findById(student.getUserId())
                .orElseThrow(() -> new UserNotFoundException(student.getUserId()));
        user.setStatus(User.Status.Inactive);
        userRepository.save(user);
    }

    private Student getStudent(Integer studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private String requireNonBlank(String value, String field) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return trimmed;
    }
}
