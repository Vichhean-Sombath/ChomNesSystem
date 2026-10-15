package com.chomnes.system.repository;

import com.chomnes.system.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    List<Certificate> findAllByOrderByIssueDateDescCertificateIdDesc();

    List<Certificate> findAllByStudentIdOrderByIssueDateDescCertificateIdDesc(Integer studentId);
}
