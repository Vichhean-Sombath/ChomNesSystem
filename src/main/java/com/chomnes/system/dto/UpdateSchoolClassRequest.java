package com.chomnes.system.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateSchoolClassRequest(
        @Size(min = 1, max = 50) String className,
        @Positive Integer academicYearId,
        @Positive Integer semesterId,
        @Size(max = 20) String roomNumber,
        @Positive Integer maxCapacity,
        @Positive Integer homeroomTeacherId,
        Boolean clearRoomNumber,
        Boolean clearHomeroomTeacher
) {
    public boolean isEmpty() {
        return className == null
                && academicYearId == null
                && semesterId == null
                && roomNumber == null
                && maxCapacity == null
                && homeroomTeacherId == null
                && clearRoomNumber == null
                && clearHomeroomTeacher == null;
    }
}
