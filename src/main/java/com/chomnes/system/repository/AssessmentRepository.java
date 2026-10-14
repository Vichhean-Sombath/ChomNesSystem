package com.chomnes.system.repository;

import com.chomnes.system.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {
    List<Assessment> findAllByOrderByAssessmentDateDescAssessmentIdDesc();

    List<Assessment> findAllByScheduleIdInOrderByAssessmentDateAscAssessmentIdAsc(List<Integer> scheduleIds);
}
