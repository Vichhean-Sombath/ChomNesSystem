package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
        @NotBlank @Size(max = 50) String className,
        @NotNull @Positive Integer academicYearId,
        @NotNull @Positive Integer semesterId,
        @Size(max = 20) String roomNumber,
        @Positive Integer maxCapacity,
        @Positive Integer homeroomTeacherId
) {
}
