package com.chomnes.system.repository;

import com.chomnes.system.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {
    boolean existsByScheduleIdAndStudentIdAndAttendanceDate(
        Integer scheduleId,
        Integer studentId,
        LocalDate attendanceDate
    );

    boolean existsByScheduleIdAndStudentIdAndAttendanceDateAndAttendanceIdNot(
        Integer scheduleId,
        Integer studentId,
        LocalDate attendanceDate,
        Integer attendanceId
    );

    List<AttendanceRecord> findAllByOrderByAttendanceDateDescAttendanceIdDesc();

    List<AttendanceRecord> findAllByStudentIdOrderByAttendanceDateDescAttendanceIdDesc(Integer studentId);

    List<AttendanceRecord> findAllByScheduleIdInOrderByAttendanceDateDescAttendanceIdDesc(List<Integer> scheduleIds);
}
