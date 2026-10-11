package com.chomnes.system.model;

import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Integer classId;

    @Column(name = "class_name", nullable = false, length = 50)
    @Nationalized
    private String className;

    @Column(name = "academic_year_id", nullable = false)
    private Integer academicYearId;

    @Column(name = "semester_id", nullable = false)
    private Integer semesterId;

    @Column(name = "room_number", length = 20)
    @Nationalized
    private String roomNumber;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "homeroom_teacher_id")
    private Integer homeroomTeacherId;

    protected SchoolClass() {
        // Required by JPA.
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Integer academicYearId) {
        this.academicYearId = academicYearId;
    }

    public Integer getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Integer semesterId) {
        this.semesterId = semesterId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getHomeroomTeacherId() {
        return homeroomTeacherId;
    }

    public void setHomeroomTeacherId(Integer homeroomTeacherId) {
        this.homeroomTeacherId = homeroomTeacherId;
    }

}
