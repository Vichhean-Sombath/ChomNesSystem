package com.chomnes.system.service;

import com.chomnes.system.dto.CreateScheduleRequest;
import com.chomnes.system.dto.ScheduleResponse;
import com.chomnes.system.dto.UpdateScheduleRequest;
import com.chomnes.system.exception.AcademicYearConflictException;
import com.chomnes.system.exception.AcademicYearNotFoundException;
import com.chomnes.system.exception.ScheduleConflictException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.exception.SubjectNotFoundException;
import com.chomnes.system.exception.TeacherNotFoundException;
import com.chomnes.system.exception.TimeSlotNotFoundException;
import com.chomnes.system.model.AcademicYear;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.Subject;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.TimeSlot;
import com.chomnes.system.repository.AcademicYearRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.SubjectRepository;
import com.chomnes.system.repository.TeacherRepository;
import com.chomnes.system.repository.TimeSlotRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            TimeSlotRepository timeSlotRepository,
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAll() {
        return scheduleRepository.findAll(Sort.by(Sort.Direction.ASC, "classId", "timeSlotId", "scheduleId"))
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse findById(Integer scheduleId) {
        return ScheduleResponse.from(getSchedule(scheduleId));
    }

    public ScheduleResponse create(CreateScheduleRequest request) {
        validateReferences(request.classId(), request.subjectId(), request.teacherId(), request.timeSlotId());
        String roomNumber = normalizeOptional(request.roomNumber());
        validateAvailability(request.classId(), request.teacherId(), request.timeSlotId(), roomNumber, null);

        Schedule schedule = new Schedule(
                request.classId(),
                request.subjectId(),
                request.teacherId(),
                request.timeSlotId(),
                roomNumber
        );
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public ScheduleResponse update(Integer scheduleId, UpdateScheduleRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one schedule field to update.");
        }
        if (Boolean.TRUE.equals(request.clearRoomNumber()) && request.roomNumber() != null) {
            throw new IllegalArgumentException("Set either roomNumber or clearRoomNumber, not both.");
        }

        Schedule schedule = getSchedule(scheduleId);
        Integer classId = request.classId() == null ? schedule.getClassId() : request.classId();
        Integer subjectId = request.subjectId() == null ? schedule.getSubjectId() : request.subjectId();
        Integer teacherId = request.teacherId() == null ? schedule.getTeacherId() : request.teacherId();
        Integer timeSlotId = request.timeSlotId() == null ? schedule.getTimeSlotId() : request.timeSlotId();
        String roomNumber = Boolean.TRUE.equals(request.clearRoomNumber())
                ? null
                : request.roomNumber() == null
                        ? schedule.getRoomNumber()
                        : normalizeOptional(request.roomNumber());

        validateReferences(classId, subjectId, teacherId, timeSlotId);
        validateAvailability(classId, teacherId, timeSlotId, roomNumber, scheduleId);

        schedule.setClassId(classId);
        schedule.setSubjectId(subjectId);
        schedule.setTeacherId(teacherId);
        schedule.setTimeSlotId(timeSlotId);
        schedule.setRoomNumber(roomNumber);
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public void delete(Integer scheduleId) {
        getSchedule(scheduleId);
        scheduleRepository.deleteById(scheduleId);
    }

    private Schedule getSchedule(Integer scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    private void validateReferences(Integer classId, Integer subjectId, Integer teacherId, Integer timeSlotId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new SchoolClassNotFoundException(classId));
        AcademicYear academicYear = academicYearRepository.findById(schoolClass.getAcademicYearId())
                .orElseThrow(() -> new AcademicYearNotFoundException(schoolClass.getAcademicYearId()));
        if (!"Active".equals(academicYear.getStatus())) {
            throw new AcademicYearConflictException("Schedules can only be assigned to an active academic year.");
        }

        Semester semester = semesterRepository.findById(schoolClass.getSemesterId())
                .orElseThrow(() -> new SemesterNotFoundException(schoolClass.getSemesterId()));
        if (!semester.getAcademicYearId().equals(academicYear.getAcademicYearId())) {
            throw new ScheduleConflictException("The class semester does not belong to its academic year.");
        }

        if (!subjectRepository.existsById(subjectId)) {
            throw new SubjectNotFoundException(subjectId);
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
        if (!"Active".equals(teacher.getStatus())) {
            throw new ScheduleConflictException("An inactive teacher cannot be assigned to a schedule.");
        }

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new TimeSlotNotFoundException(timeSlotId));
        if (!timeSlot.getEndTime().isAfter(timeSlot.getStartTime())) {
            throw new ScheduleConflictException("The selected time slot has an invalid time range.");
        }
    }

    private void validateAvailability(
            Integer classId,
            Integer teacherId,
            Integer timeSlotId,
            String roomNumber,
            Integer excludedScheduleId
    ) {
        boolean teacherConflict = excludedScheduleId == null
                ? scheduleRepository.existsByTeacherIdAndTimeSlotId(teacherId, timeSlotId)
                : scheduleRepository.existsByTeacherIdAndTimeSlotIdAndScheduleIdNot(
                        teacherId, timeSlotId, excludedScheduleId
                );
        if (teacherConflict) {
            throw new ScheduleConflictException("The teacher already has a schedule in this time slot.");
        }

        boolean classConflict = excludedScheduleId == null
                ? scheduleRepository.existsByClassIdAndTimeSlotId(classId, timeSlotId)
                : scheduleRepository.existsByClassIdAndTimeSlotIdAndScheduleIdNot(
                        classId, timeSlotId, excludedScheduleId
                );
        if (classConflict) {
            throw new ScheduleConflictException("The class already has a schedule in this time slot.");
        }

        if (roomNumber != null) {
            boolean roomConflict = excludedScheduleId == null
                    ? scheduleRepository.existsByRoomNumberAndTimeSlotId(roomNumber, timeSlotId)
                    : scheduleRepository.existsByRoomNumberAndTimeSlotIdAndScheduleIdNot(
                            roomNumber, timeSlotId, excludedScheduleId
                    );
            if (roomConflict) {
                throw new ScheduleConflictException("The room is already booked in this time slot.");
            }
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
