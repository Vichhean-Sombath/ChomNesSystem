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
@Table(name = "final_grades")
public class FinalGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "final_grade_id")
    private Integer finalGradeId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "semester_id", nullable = false)
    private Integer semesterId;

    @Column(name = "final_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "letter_grade", length = 2)
    @Nationalized
    private String letterGrade;

    @Column(name = "gpa_points", precision = 3, scale = 2)
    private BigDecimal gpaPoints;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (computedAt == null) {
            computedAt = LocalDateTime.now();
        }
    }

    protected FinalGrade() {
        // Required by JPA.
    }

    public FinalGrade(
            Integer studentId,
            Integer subjectId,
            Integer classId,
            Integer semesterId,
            BigDecimal finalScore
    ) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.classId = classId;
        this.semesterId = semesterId;
        this.finalScore = finalScore;
    }

    public Integer getFinalGradeId() {
        return finalGradeId;
    }

    public void setFinalGradeId(Integer finalGradeId) {
        this.finalGradeId = finalGradeId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
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

    public Integer getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Integer semesterId) {
        this.semesterId = semesterId;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public BigDecimal getGpaPoints() {
        return gpaPoints;
    }

    public void setGpaPoints(BigDecimal gpaPoints) {
        this.gpaPoints = gpaPoints;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

}
