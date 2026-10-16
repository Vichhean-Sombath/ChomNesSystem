package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

public record CreateTimeSlotRequest(
    @NotBlank @Pattern(regexp = "Mon|Tue|Wed|Thu|Fri|Sat|Sun") String dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {
}
