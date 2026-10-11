package com.chomnes.system.model;

import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Integer gradeId;

    @Column(name = "assessment_id", nullable = false)
    private Integer assessmentId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "score", nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "entered_by", nullable = false)
    private Integer enteredBy;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (enteredAt == null) {
            enteredAt = LocalDateTime.now();
        }
    }

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    protected Grade() {
        // Required by JPA.
    }

    public Integer getGradeId() {
        return gradeId;
    }

    public void setGradeId(Integer gradeId) {
        this.gradeId = gradeId;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Integer getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(Integer enteredBy) {
        this.enteredBy = enteredBy;
    }

    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(LocalDateTime enteredAt) {
        this.enteredAt = enteredAt;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

}
