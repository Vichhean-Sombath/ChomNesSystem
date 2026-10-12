package com.chomnes.system.dto;

import com.chomnes.system.model.AcademicYear;

import java.time.LocalDate;

public record AcademicYearResponse(
        Integer academicYearId,
        String yearName,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
    public static AcademicYearResponse from(AcademicYear academicYear) {
        return new AcademicYearResponse(
                academicYear.getAcademicYearId(),
                academicYear.getYearName(),
                academicYear.getStartDate(),
                academicYear.getEndDate(),
                academicYear.getStatus()
        );
    }
}
