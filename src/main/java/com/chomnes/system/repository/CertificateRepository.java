package com.chomnes.system.repository;

import com.chomnes.system.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    Optional<Certificate> findByGeneratedFileUrl(String generatedFileUrl);

    List<Certificate> findAllByOrderByIssueDateDescCertificateIdDesc();

    List<Certificate> findAllByStudentIdOrderByIssueDateDescCertificateIdDesc(Integer studentId);
}
