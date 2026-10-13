package com.chomnes.system.service;

import com.chomnes.system.dto.CreateTimeSlotRequest;
import com.chomnes.system.dto.TimeSlotResponse;
import com.chomnes.system.dto.UpdateTimeSlotRequest;
import com.chomnes.system.exception.TimeSlotConflictException;
import com.chomnes.system.exception.TimeSlotNotFoundException;
import com.chomnes.system.model.TimeSlot;
import com.chomnes.system.repository.TimeSlotRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class TimeSlotService {
    private static final List<String> DAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findAll() {
        return timeSlotRepository.findAll(Sort.by(Sort.Direction.ASC, "dayOfWeek", "startTime"))
                .stream()
                .map(TimeSlotResponse::from)
                .sorted((left, right) -> {
                    int dayOrder = Integer.compare(DAYS.indexOf(left.dayOfWeek()), DAYS.indexOf(right.dayOfWeek()));
                    return dayOrder != 0 ? dayOrder : left.startTime().compareTo(right.startTime());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TimeSlotResponse findById(Integer timeSlotId) {
        return TimeSlotResponse.from(getTimeSlot(timeSlotId));
    }

    public TimeSlotResponse create(CreateTimeSlotRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        ensureNoOverlap(request.dayOfWeek(), request.startTime(), request.endTime(), null);
        TimeSlot timeSlot = new TimeSlot(request.dayOfWeek(), request.startTime(), request.endTime());
        return TimeSlotResponse.from(timeSlotRepository.save(timeSlot));
    }

    public TimeSlotResponse update(Integer timeSlotId, UpdateTimeSlotRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one time slot field to update.");
        }
        TimeSlot timeSlot = getTimeSlot(timeSlotId);
        String dayOfWeek = request.dayOfWeek() == null ? timeSlot.getDayOfWeek() : request.dayOfWeek();
        LocalTime startTime = request.startTime() == null ? timeSlot.getStartTime() : request.startTime();
        LocalTime endTime = request.endTime() == null ? timeSlot.getEndTime() : request.endTime();
        validateTimeRange(startTime, endTime);
        ensureNoOverlap(dayOfWeek, startTime, endTime, timeSlotId);

        timeSlot.setDayOfWeek(dayOfWeek);
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        return TimeSlotResponse.from(timeSlotRepository.save(timeSlot));
    }

    private TimeSlot getTimeSlot(Integer timeSlotId) {
        return timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new TimeSlotNotFoundException(timeSlotId));
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new IllegalArgumentException("Time slots must use whole-second precision.");
        }
    }

    private void ensureNoOverlap(String dayOfWeek, LocalTime startTime, LocalTime endTime, Integer excludedId) {
        for (TimeSlot existing : timeSlotRepository.findAllByDayOfWeek(dayOfWeek)) {
            if (existing.getTimeSlotId().equals(excludedId)) {
                continue;
            }
            boolean overlaps = startTime.isBefore(existing.getEndTime())
                    && endTime.isAfter(existing.getStartTime());
            if (overlaps) {
                throw new TimeSlotConflictException("This time slot overlaps another slot on " + dayOfWeek + ".");
            }
        }
    }
}
