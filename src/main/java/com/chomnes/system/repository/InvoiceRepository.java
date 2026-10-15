package com.chomnes.system.repository;

import com.chomnes.system.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    List<Invoice> findAllByOrderByCreatedAtDescInvoiceIdDesc();

    List<Invoice> findAllByStudentIdOrderByCreatedAtDescInvoiceIdDesc(Integer studentId);

    boolean existsByStudentIdAndFeeId(Integer studentId, Integer feeId);
}
