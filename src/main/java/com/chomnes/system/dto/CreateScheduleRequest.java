package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateScheduleRequest(
    @NotNull @Positive Integer classId,
    @NotNull @Positive Integer subjectId,
    @NotNull @Positive Integer teacherId,
    @NotNull @Positive Integer timeSlotId,
    @Size(max = 20) String roomNumber
) {
}
