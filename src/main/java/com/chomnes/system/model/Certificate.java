package com.chomnes.system.model;

import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Integer certificateId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "certificate_type", nullable = false, length = 20)
    @Nationalized
    private String certificateType;

    @Column(name = "template_used", length = 100)
    @Nationalized
    private String templateUsed;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "generated_file_url", length = 255)
    @Nationalized
    private String generatedFileUrl;

    @Column(name = "issued_by", nullable = false)
    private Integer issuedBy;

    protected Certificate() {
        // Required by JPA.
    }

    public Certificate(
            Integer studentId,
            String certificateType,
            String templateUsed,
            LocalDate issueDate,
            String generatedFileUrl,
            Integer issuedBy
    ) {
        this.studentId = studentId;
        this.certificateType = certificateType;
        this.templateUsed = templateUsed;
        this.issueDate = issueDate;
        this.generatedFileUrl = generatedFileUrl;
        this.issuedBy = issuedBy;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getTemplateUsed() {
        return templateUsed;
    }

    public void setTemplateUsed(String templateUsed) {
        this.templateUsed = templateUsed;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getGeneratedFileUrl() {
        return generatedFileUrl;
    }

    public void setGeneratedFileUrl(String generatedFileUrl) {
        this.generatedFileUrl = generatedFileUrl;
    }

    public Integer getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Integer issuedBy) {
        this.issuedBy = issuedBy;
    }

}
