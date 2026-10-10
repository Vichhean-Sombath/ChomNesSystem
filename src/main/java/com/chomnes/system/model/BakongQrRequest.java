package com.chomnes.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "bakong_qr_requests")
public class BakongQrRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_id")
    private Integer qrId;

    @Column(name = "invoice_id", nullable = false)
    private Integer invoiceId;

    @Column(name = "md5", nullable = false, length = 64)
    private String md5;

    @Column(name = "amount_khr", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountKhr;

    @Column(name = "amount_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "status", nullable = false, length = 10)
    private String status = "Pending";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    protected BakongQrRequest() {
        // Required by JPA.
    }

    public Integer getQrId() {
        return qrId;
    }

    public void setQrId(Integer qrId) {
        this.qrId = qrId;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public BigDecimal getAmountKhr() {
        return amountKhr;
    }

    public void setAmountKhr(BigDecimal amountKhr) {
        this.amountKhr = amountKhr;
    }

    public BigDecimal getAmountUsd() {
        return amountUsd;
    }

    public void setAmountUsd(BigDecimal amountUsd) {
        this.amountUsd = amountUsd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
