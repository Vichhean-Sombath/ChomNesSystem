package com.chomnes.system.dto;

import com.chomnes.system.model.GradingCriterion;

import java.math.BigDecimal;

public record GradingCriterionResponse(
        Integer criteriaId,
        Integer subjectId,
        Integer classId,
        String componentName,
        BigDecimal weightPercentage,
        Integer attemptCount
) {
    public static GradingCriterionResponse from(GradingCriterion criterion) {
        return new GradingCriterionResponse(
                criterion.getCriteriaId(),
                criterion.getSubjectId(),
                criterion.getClassId(),
                criterion.getComponentName(),
                criterion.getWeightPercentage(),
                criterion.getAttemptCount()
        );
    }
}
