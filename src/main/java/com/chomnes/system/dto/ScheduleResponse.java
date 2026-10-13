package com.chomnes.system.dto;

import com.chomnes.system.model.Schedule;

public record ScheduleResponse(
        Integer scheduleId,
        Integer classId,
        Integer subjectId,
        Integer teacherId,
        Integer timeSlotId,
        String roomNumber
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getClassId(),
                schedule.getSubjectId(),
                schedule.getTeacherId(),
                schedule.getTimeSlotId(),
                schedule.getRoomNumber()
        );
    }
}
