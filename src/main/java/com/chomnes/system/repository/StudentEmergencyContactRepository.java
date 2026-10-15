package com.chomnes.system.repository;

import com.chomnes.system.model.StudentEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentEmergencyContactRepository extends JpaRepository<StudentEmergencyContact, Integer> {
    List<StudentEmergencyContact> findAllByOrderByContactNameAscContactIdAsc();

    List<StudentEmergencyContact> findAllByStudentIdOrderByContactNameAscContactIdAsc(Integer studentId);
}
