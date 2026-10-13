package com.chomnes.system.repository;

import com.chomnes.system.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {
    List<TimeSlot> findAllByDayOfWeek(String dayOfWeek);
}
