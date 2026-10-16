package com.chomnes.system.repository;

import com.chomnes.system.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findAllByOrderByPaymentDateDescPaymentIdDesc();

    List<Payment> findAllByInvoiceIdInOrderByPaymentDateDescPaymentIdDesc(List<Integer> invoiceIds);

    List<Payment> findAllByInvoiceIdOrderByPaymentDateDescPaymentIdDesc(Integer invoiceId);

    boolean existsByPaymentMethodAndTransactionReference(String paymentMethod, String transactionReference);

    boolean existsByTransactionReferenceAndPaymentIdNot(String transactionReference, Integer paymentId);

    boolean existsByTransactionReference(String transactionReference);

    java.util.Optional<Payment> findByTransactionReference(String transactionReference);
}
