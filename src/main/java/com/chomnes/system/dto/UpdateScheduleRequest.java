package com.chomnes.system.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateScheduleRequest(
        @Positive Integer classId,
        @Positive Integer subjectId,
        @Positive Integer teacherId,
        @Positive Integer timeSlotId,
        @Size(max = 20) String roomNumber,
        Boolean clearRoomNumber
) {
    public boolean isEmpty() {
        return classId == null
                && subjectId == null
                && teacherId == null
                && timeSlotId == null
                && roomNumber == null
                && clearRoomNumber == null;
    }
}
