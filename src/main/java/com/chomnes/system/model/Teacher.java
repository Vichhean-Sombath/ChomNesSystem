package com.chomnes.system.model;

import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_id")
    private Integer teacherId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "first_name", nullable = false, length = 50)
    @Nationalized
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    @Nationalized
    private String lastName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender", length = 6)
    @Nationalized
    private String gender;

    @Column(name = "photo_url", length = 255)
    @Nationalized
    private String photoUrl;

    @Column(name = "contact_number", length = 20)
    @Nationalized
    private String contactNumber;

    @Column(name = "specialization", length = 100)
    @Nationalized
    private String specialization;

    @Column(name = "bio", columnDefinition = "nvarchar(max)")
    @Nationalized
    private String bio;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "status", nullable = false, length = 20)
    @Nationalized
    private String status = "Active";

    protected Teacher() {
        // Required by JPA.
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
