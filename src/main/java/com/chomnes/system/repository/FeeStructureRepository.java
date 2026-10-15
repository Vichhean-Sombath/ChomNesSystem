package com.chomnes.system.repository;

import com.chomnes.system.model.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Integer> {
    List<FeeStructure> findAllByOrderBySemesterIdAscFeeIdAsc();
}
