package com.chomnes.system.model;

import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "grading_criteria")
public class GradingCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criteria_id")
    private Integer criteriaId;

    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(name = "class_id")
    private Integer classId;

    @Column(name = "component_name", nullable = false, length = 50)
    @Nationalized
    private String componentName;

    @Column(name = "weight_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightPercentage;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    protected GradingCriterion() {
        // Required by JPA.
    }

    public GradingCriterion(
        Integer subjectId,
        Integer classId,
        String componentName,
        BigDecimal weightPercentage,
        Integer attemptCount
    ) {
        this.subjectId = subjectId;
        this.classId = classId;
        this.componentName = componentName;
        this.weightPercentage = weightPercentage;
        this.attemptCount = attemptCount;
    }

    public Integer getCriteriaId() {
        return criteriaId;
    }

    public void setCriteriaId(Integer criteriaId) {
        this.criteriaId = criteriaId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public BigDecimal getWeightPercentage() {
        return weightPercentage;
    }

    public void setWeightPercentage(BigDecimal weightPercentage) {
        this.weightPercentage = weightPercentage;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

}
