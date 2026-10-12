package com.chomnes.system.repository;

import com.chomnes.system.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SemesterRepository extends JpaRepository<Semester, Integer> {
    List<Semester> findAllByAcademicYearIdOrderByStartDateAsc(Integer academicYearId);
}
