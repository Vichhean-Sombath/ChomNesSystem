package com.chomnes.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(name = "subject_code", nullable = false, length = 20)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "description", columnDefinition = "varchar(max)")
    private String description;

    @Column(name = "prerequisite_subject_id")
    private Integer prerequisiteSubjectId;

    protected Subject() {
        // Required by JPA.
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPrerequisiteSubjectId() {
        return prerequisiteSubjectId;
    }

    public void setPrerequisiteSubjectId(Integer prerequisiteSubjectId) {
        this.prerequisiteSubjectId = prerequisiteSubjectId;
    }

}
