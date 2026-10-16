package com.chomnes.system.dto;

import com.chomnes.system.model.TimeSlot;

import java.time.LocalTime;

public record TimeSlotResponse(
    Integer timeSlotId,
    String dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
    public static TimeSlotResponse from(TimeSlot timeSlot) {
        return new TimeSlotResponse(
            timeSlot.getTimeSlotId(),
            timeSlot.getDayOfWeek(),
            timeSlot.getStartTime(),
            timeSlot.getEndTime()
        );
    }
}
