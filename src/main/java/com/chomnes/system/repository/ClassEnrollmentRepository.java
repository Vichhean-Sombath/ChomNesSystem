package com.chomnes.system.repository;

import com.chomnes.system.model.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Integer> {
    long countByClassIdAndStatus(Integer classId, String status);

    boolean existsByStudentIdAndClassIdAndStatus(Integer studentId, Integer classId, String status);

    boolean existsByStudentIdAndClassId(Integer studentId, Integer classId);

    boolean existsByStudentIdAndClassIdAndStatusAndEnrollmentDateLessThanEqual(
        Integer studentId,
        Integer classId,
        String status,
        LocalDate enrollmentDate
    );

    boolean existsByStudentIdAndClassIdAndStatusAndEnrollmentIdNot(
        Integer studentId,
        Integer classId,
        String status,
        Integer enrollmentId
    );

    List<ClassEnrollment> findAllByOrderByEnrollmentDateDescEnrollmentIdAsc();

    List<ClassEnrollment> findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(Integer studentId);
}
