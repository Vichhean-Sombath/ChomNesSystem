package com.chomnes.system.repository;

import com.chomnes.system.model.BakongQrRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BakongQrRequestRepository extends JpaRepository<BakongQrRequest, Integer> {
    List<BakongQrRequest> findAllByOrderByCreatedAtDescQrIdDesc();

    List<BakongQrRequest> findAllByInvoiceIdInOrderByCreatedAtDescQrIdDesc(List<Integer> invoiceIds);

    boolean existsByInvoiceIdAndStatus(Integer invoiceId, String status);

    List<BakongQrRequest> findAllByInvoiceIdAndStatus(Integer invoiceId, String status);

    Optional<BakongQrRequest> findByMd5(String md5);
}
