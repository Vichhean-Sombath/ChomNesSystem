package com.chomnes.system.repository;

import com.chomnes.system.model.StudentEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentEmergencyContactRepository extends JpaRepository<StudentEmergencyContact, Integer> {
    Optional<StudentEmergencyContact> findFirstByStudentIdAndEmailIsNotNullOrderByContactIdAsc(Integer studentId);

    List<StudentEmergencyContact> findAllByOrderByContactNameAscContactIdAsc();

    List<StudentEmergencyContact> findAllByStudentIdOrderByContactNameAscContactIdAsc(Integer studentId);
}
