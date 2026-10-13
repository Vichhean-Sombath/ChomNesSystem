package com.chomnes.system.repository;

import com.chomnes.system.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
    boolean existsByTeacherIdAndTimeSlotId(Integer teacherId, Integer timeSlotId);

    boolean existsByTeacherIdAndTimeSlotIdAndScheduleIdNot(Integer teacherId, Integer timeSlotId, Integer scheduleId);

    boolean existsByClassIdAndTimeSlotId(Integer classId, Integer timeSlotId);

    boolean existsByClassIdAndTimeSlotIdAndScheduleIdNot(Integer classId, Integer timeSlotId, Integer scheduleId);

    boolean existsByRoomNumberAndTimeSlotId(String roomNumber, Integer timeSlotId);

    boolean existsByRoomNumberAndTimeSlotIdAndScheduleIdNot(
            String roomNumber,
            Integer timeSlotId,
            Integer scheduleId
    );
}
