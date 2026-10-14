package com.chomnes.system.exception;

public class GradingCriterionNotFoundException extends RuntimeException {
    public GradingCriterionNotFoundException(Integer criteriaId) {
        super("Grading criterion " + criteriaId + " was not found.");
    }
}
