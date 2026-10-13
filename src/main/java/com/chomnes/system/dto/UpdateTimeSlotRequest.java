package com.chomnes.system.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

public record UpdateTimeSlotRequest(
        @Pattern(regexp = "Mon|Tue|Wed|Thu|Fri|Sat|Sun") String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public boolean isEmpty() {
        return dayOfWeek == null && startTime == null && endTime == null;
    }
}
