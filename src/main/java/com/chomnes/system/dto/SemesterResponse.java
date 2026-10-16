package com.chomnes.system.dto;

import com.chomnes.system.model.Semester;

import java.time.LocalDate;

public record SemesterResponse(
    Integer semesterId,
    Integer academicYearId,
    String semesterName,
    LocalDate startDate,
    LocalDate endDate
) {
    public static SemesterResponse from(Semester semester) {
        return new SemesterResponse(
            semester.getSemesterId(),
            semester.getAcademicYearId(),
            semester.getSemesterName(),
            semester.getStartDate(),
            semester.getEndDate()
        );
    }
}
