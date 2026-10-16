package com.chomnes.system.service;

import com.chomnes.system.dto.CertificateResponse;
import com.chomnes.system.dto.CreateCertificateRequest;
import com.chomnes.system.exception.CertificateNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.model.Certificate;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.CertificateRepository;
import com.chomnes.system.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CertificateService {
    private final CertificateRepository certificateRepository;
    private final StudentRepository studentRepository;

    public CertificateService(CertificateRepository certificateRepository, StudentRepository studentRepository) {
        this.certificateRepository = certificateRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return certificateRepository.findAllByOrderByIssueDateDescCertificateIdDesc().stream()
                    .map(CertificateResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            return certificateRepository.findAllByStudentIdOrderByIssueDateDescCertificateIdDesc(
                            student.getStudentId()
                    ).stream()
                    .map(CertificateResponse::from)
                    .toList();
        }
        throw new AccessDeniedException("You cannot access certificates.");
    }

    @Transactional(readOnly = true)
    public CertificateResponse findById(Integer certificateId, Integer requesterId, User.Role requesterRole) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new CertificateNotFoundException(certificateId));
        if (requesterRole == User.Role.Admin) {
            return CertificateResponse.from(certificate);
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!student.getStudentId().equals(certificate.getStudentId())) {
                throw new AccessDeniedException("Students can only access their own certificates.");
            }
            return CertificateResponse.from(certificate);
        }
        throw new AccessDeniedException("You cannot access certificates.");
    }

    public CertificateResponse issue(CreateCertificateRequest request, Integer requesterId, User.Role requesterRole) {
        if (requesterRole != User.Role.Admin) {
            throw new AccessDeniedException("Only an administrator can issue certificates.");
        }
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(request.studentId()));
        Certificate certificate = new Certificate(
                student.getStudentId(),
                request.certificateType(),
                request.templateUsed(),
                request.issueDate(),
                request.generatedFileUrl(),
                requesterId
        );
        return CertificateResponse.from(certificateRepository.save(certificate));
    }

    public void delete(Integer certificateId) {
        certificateRepository.findById(certificateId)
                .orElseThrow(() -> new CertificateNotFoundException(certificateId));
        certificateRepository.deleteById(certificateId);
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }
}
