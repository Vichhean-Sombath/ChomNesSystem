package com.chomnes.system.dto;

import com.chomnes.system.model.Certificate;

import java.time.LocalDate;

public record CertificateResponse(
        Integer certificateId,
        Integer studentId,
        String certificateType,
        String templateUsed,
        LocalDate issueDate,
        String generatedFileUrl,
        Integer issuedBy
) {
    public static CertificateResponse from(Certificate certificate) {
        return new CertificateResponse(
                certificate.getCertificateId(),
                certificate.getStudentId(),
                certificate.getCertificateType(),
                certificate.getTemplateUsed(),
                certificate.getIssueDate(),
                certificate.getGeneratedFileUrl(),
                certificate.getIssuedBy()
        );
    }
}
