package com.chomnes.system.dto;

import com.chomnes.system.model.Assessment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssessmentResponse(
    Integer assessmentId,
    Integer scheduleId,
    Integer criteriaId,
    String assessmentName,
    BigDecimal maxScore,
    LocalDate assessmentDate
) {
    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
            assessment.getAssessmentId(),
            assessment.getScheduleId(),
            assessment.getCriteriaId(),
            assessment.getAssessmentName(),
            assessment.getMaxScore(),
            assessment.getAssessmentDate()
        );
    }
}
