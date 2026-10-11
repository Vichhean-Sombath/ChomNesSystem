package com.chomnes.system.repository;

import com.chomnes.system.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {
}
