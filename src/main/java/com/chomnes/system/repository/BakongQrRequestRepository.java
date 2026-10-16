package com.chomnes.system.repository;

import com.chomnes.system.model.BakongQrRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface BakongQrRequestRepository extends JpaRepository<BakongQrRequest, Integer> {
    List<BakongQrRequest> findAllByOrderByCreatedAtDescQrIdDesc();

    List<BakongQrRequest> findAllByInvoiceIdInOrderByCreatedAtDescQrIdDesc(List<Integer> invoiceIds);

    boolean existsByInvoiceIdAndStatus(Integer invoiceId, String status);

    List<BakongQrRequest> findAllByInvoiceIdAndStatus(Integer invoiceId, String status);

    Optional<BakongQrRequest> findByMd5(String md5);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select qr from BakongQrRequest qr where qr.md5 = :md5")
    Optional<BakongQrRequest> findByMd5ForUpdate(@Param("md5") String md5);
}
