package com.chomnes.system.repository;

import com.chomnes.system.model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Integer> {
    boolean existsByYearName(String yearName);

    boolean existsByYearNameAndAcademicYearIdNot(String yearName, Integer academicYearId);
}
