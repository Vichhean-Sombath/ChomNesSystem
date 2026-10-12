package com.chomnes.system.exception;

public class AcademicYearNotFoundException extends RuntimeException {
    public AcademicYearNotFoundException(Integer academicYearId) {
        super("Academic year " + academicYearId + " was not found.");
    }
}
