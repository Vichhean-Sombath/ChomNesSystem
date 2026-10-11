package com.chomnes.system.repository;

import com.chomnes.system.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {
}
