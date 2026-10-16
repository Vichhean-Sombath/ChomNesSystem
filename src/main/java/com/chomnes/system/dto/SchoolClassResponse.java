package com.chomnes.system.dto;

import com.chomnes.system.model.SchoolClass;

public record SchoolClassResponse(
    Integer classId,
    String className,
    Integer academicYearId,
    Integer semesterId,
    String roomNumber,
    Integer maxCapacity,
    Integer homeroomTeacherId
) {
    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(
            schoolClass.getClassId(),
            schoolClass.getClassName(),
            schoolClass.getAcademicYearId(),
            schoolClass.getSemesterId(),
            schoolClass.getRoomNumber(),
            schoolClass.getMaxCapacity(),
            schoolClass.getHomeroomTeacherId()
        );
    }
}
