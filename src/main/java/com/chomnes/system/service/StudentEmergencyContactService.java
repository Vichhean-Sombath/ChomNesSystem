package com.chomnes.system.service;

import com.chomnes.system.dto.CreateStudentEmergencyContactRequest;
import com.chomnes.system.dto.StudentEmergencyContactResponse;
import com.chomnes.system.dto.UpdateStudentEmergencyContactRequest;
import com.chomnes.system.exception.StudentEmergencyContactNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.StudentEmergencyContact;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.StudentEmergencyContactRepository;
import com.chomnes.system.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentEmergencyContactService {
    private final StudentEmergencyContactRepository contactRepository;
    private final StudentRepository studentRepository;

    public StudentEmergencyContactService(
        StudentEmergencyContactRepository contactRepository,
        StudentRepository studentRepository
    ) {
        this.contactRepository = contactRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentEmergencyContactResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return contactRepository.findAllByOrderByContactNameAscContactIdAsc().stream()
                .map(StudentEmergencyContactResponse::from)
                .toList();
        }
        Student student = requireStudentOwner(requesterId, requesterRole);
        return contactRepository.findAllByStudentIdOrderByContactNameAscContactIdAsc(student.getStudentId())
            .stream()
            .map(StudentEmergencyContactResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public StudentEmergencyContactResponse findById(
        Integer contactId,
        Integer requesterId,
        User.Role requesterRole
    ) {
        StudentEmergencyContact contact = getContact(contactId);
        authorize(contact, requesterId, requesterRole);
        return StudentEmergencyContactResponse.from(contact);
    }

    public StudentEmergencyContactResponse create(
        CreateStudentEmergencyContactRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        Student student;
        if (requesterRole == User.Role.Admin) {
            if (request.studentId() == null) {
                throw new IllegalArgumentException("studentId is required when an administrator creates a contact.");
            }
            student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(request.studentId()));
        } else {
            student = requireStudentOwner(requesterId, requesterRole);
            if (request.studentId() != null && !student.getStudentId().equals(request.studentId())) {
                throw new AccessDeniedException("Students can only create contacts for their own profile.");
            }
        }

        StudentEmergencyContact contact = new StudentEmergencyContact(
            student.getStudentId(),
            requireNonBlank(request.contactName(), "Contact name"),
            optionalText(request.relationship()),
            requireNonBlank(request.phoneNumber(), "Phone number"),
            optionalText(request.email())
        );
        return StudentEmergencyContactResponse.from(contactRepository.save(contact));
    }

    public StudentEmergencyContactResponse update(
        Integer contactId,
        UpdateStudentEmergencyContactRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one contact field to update.");
        }
        if (Boolean.TRUE.equals(request.clearRelationship()) && request.relationship() != null) {
            throw new IllegalArgumentException("Set either relationship or clearRelationship, not both.");
        }
        if (Boolean.TRUE.equals(request.clearEmail()) && request.email() != null) {
            throw new IllegalArgumentException("Set either email or clearEmail, not both.");
        }

        StudentEmergencyContact contact = getContact(contactId);
        authorize(contact, requesterId, requesterRole);
        if (request.contactName() != null) {
            contact.setContactName(requireNonBlank(request.contactName(), "Contact name"));
        }
        if (Boolean.TRUE.equals(request.clearRelationship())) {
            contact.setRelationship(null);
        } else if (request.relationship() != null) {
            contact.setRelationship(optionalText(request.relationship()));
        }
        if (request.phoneNumber() != null) {
            contact.setPhoneNumber(requireNonBlank(request.phoneNumber(), "Phone number"));
        }
        if (Boolean.TRUE.equals(request.clearEmail())) {
            contact.setEmail(null);
        } else if (request.email() != null) {
            contact.setEmail(optionalText(request.email()));
        }
        return StudentEmergencyContactResponse.from(contactRepository.save(contact));
    }

    public void delete(Integer contactId, Integer requesterId, User.Role requesterRole) {
        StudentEmergencyContact contact = getContact(contactId);
        authorize(contact, requesterId, requesterRole);
        contactRepository.delete(contact);
    }

    private StudentEmergencyContact getContact(Integer contactId) {
        return contactRepository.findById(contactId)
            .orElseThrow(() -> new StudentEmergencyContactNotFoundException(contactId));
    }

    private void authorize(StudentEmergencyContact contact, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        Student student = requireStudentOwner(requesterId, requesterRole);
        if (!student.getStudentId().equals(contact.getStudentId())) {
            throw new AccessDeniedException("Students can only access contacts for their own profile.");
        }
    }

    private Student requireStudentOwner(Integer requesterId, User.Role requesterRole) {
        if (requesterRole != User.Role.Student) {
            throw new AccessDeniedException("Only an administrator or the student can access emergency contacts.");
        }
        Student student = studentRepository.findByUserId(requesterId)
            .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
        if (!"Active".equals(student.getStatus())) {
            throw new AccessDeniedException("This student profile is inactive.");
        }
        return student;
    }

    private String requireNonBlank(String value, String fieldName) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return trimmed;
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
