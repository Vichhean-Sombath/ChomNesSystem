package com.chomnes.system.repository;

import com.chomnes.system.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    List<Invoice> findAllByOrderByCreatedAtDescInvoiceIdDesc();

    List<Invoice> findAllByStudentIdOrderByCreatedAtDescInvoiceIdDesc(Integer studentId);

    boolean existsByStudentIdAndFeeId(Integer studentId, Integer feeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.invoiceId = :invoiceId")
    java.util.Optional<Invoice> findByIdForUpdate(@Param("invoiceId") Integer invoiceId);
}
