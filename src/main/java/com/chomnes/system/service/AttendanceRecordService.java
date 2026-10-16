package com.chomnes.system.service;

import com.chomnes.system.dto.AttendanceRecordResponse;
import com.chomnes.system.dto.CreateAttendanceRecordRequest;
import com.chomnes.system.dto.UpdateAttendanceRecordRequest;
import com.chomnes.system.exception.AttendanceRecordConflictException;
import com.chomnes.system.exception.AttendanceRecordNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.exception.TeacherNotFoundException;
import com.chomnes.system.model.AttendanceRecord;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.AttendanceRecordRepository;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AttendanceRecordService {
    private final AttendanceRecordRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ScheduleRepository scheduleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SemesterRepository semesterRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public AttendanceRecordService(
        AttendanceRecordRepository attendanceRepository,
        StudentRepository studentRepository,
        TeacherRepository teacherRepository,
        ScheduleRepository scheduleRepository,
        SchoolClassRepository schoolClassRepository,
        SemesterRepository semesterRepository,
        ClassEnrollmentRepository enrollmentRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.scheduleRepository = scheduleRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.semesterRepository = semesterRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> findAll(Integer requesterId, User.Role requesterRole) {
        List<AttendanceRecord> records;
        if (requesterRole == User.Role.Admin) {
            records = attendanceRepository.findAllByOrderByAttendanceDateDescAttendanceIdDesc();
        } else if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            List<Integer> scheduleIds = scheduleRepository.findAllByTeacherId(teacher.getTeacherId())
                .stream()
                .map(Schedule::getScheduleId)
                .toList();
            if (scheduleIds.isEmpty()) {
                return List.of();
            }
            records = attendanceRepository.findAllByScheduleIdInOrderByAttendanceDateDescAttendanceIdDesc(
                scheduleIds
            );
        } else if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            records = attendanceRepository.findAllByStudentIdOrderByAttendanceDateDescAttendanceIdDesc(
                student.getStudentId()
            );
        } else {
            throw new AccessDeniedException("You cannot access attendance records.");
        }
        return records.stream().map(AttendanceRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceRecordResponse findById(Integer attendanceId, Integer requesterId, User.Role requesterRole) {
        AttendanceRecord record = getAttendanceRecord(attendanceId);
        authorizeRead(record, requesterId, requesterRole);
        return AttendanceRecordResponse.from(record);
    }

    public AttendanceRecordResponse create(
        CreateAttendanceRecordRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        Schedule schedule = getSchedule(request.scheduleId());
        Teacher marker = getAuthorizedMarker(schedule, requesterId, requesterRole);
        Student student = getStudent(request.studentId());
        if (!"Active".equals(student.getStatus())) {
            throw new AttendanceRecordConflictException("An inactive student cannot have attendance recorded.");
        }

        LocalDate attendanceDate = request.attendanceDate() == null ? LocalDate.now() : request.attendanceDate();
        validateStudentEnrollment(student.getStudentId(), schedule.getClassId(), attendanceDate);
        validateAttendanceDate(schedule, attendanceDate);
        if (attendanceRepository.existsByScheduleIdAndStudentIdAndAttendanceDate(
            schedule.getScheduleId(), student.getStudentId(), attendanceDate
        )) {
            throw new AttendanceRecordConflictException("Attendance has already been recorded for this student and date.");
        }

        AttendanceRecord record = new AttendanceRecord(
            schedule.getScheduleId(),
            student.getStudentId(),
            attendanceDate,
            request.status(),
            marker.getTeacherId()
        );
        return AttendanceRecordResponse.from(attendanceRepository.save(record));
    }

    public AttendanceRecordResponse update(
        Integer attendanceId,
        UpdateAttendanceRecordRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one attendance field to update.");
        }
        AttendanceRecord record = getAttendanceRecord(attendanceId);
        authorizeWrite(record, requesterId, requesterRole);

        LocalDate attendanceDate = request.attendanceDate() == null
            ? record.getAttendanceDate()
            : request.attendanceDate();
        String status = request.status() == null ? record.getStatus() : request.status();
        Schedule schedule = getSchedule(record.getScheduleId());
        validateStudentEnrollment(record.getStudentId(), schedule.getClassId(), attendanceDate);
        validateAttendanceDate(schedule, attendanceDate);
        if (!attendanceDate.equals(record.getAttendanceDate())
            && attendanceRepository.existsByScheduleIdAndStudentIdAndAttendanceDateAndAttendanceIdNot(
            record.getScheduleId(), record.getStudentId(), attendanceDate, attendanceId
        )) {
            throw new AttendanceRecordConflictException(
                "Attendance has already been recorded for this student and date."
            );
        }

        record.setAttendanceDate(attendanceDate);
        record.setStatus(status);
        return AttendanceRecordResponse.from(attendanceRepository.save(record));
    }

    private void authorizeRead(AttendanceRecord record, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!student.getStudentId().equals(record.getStudentId())) {
                throw new AccessDeniedException("Students can only access their own attendance records.");
            }
            return;
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            Schedule schedule = getSchedule(record.getScheduleId());
            if (!teacher.getTeacherId().equals(schedule.getTeacherId())) {
                throw new AccessDeniedException("Teachers can only access attendance for their own schedules.");
            }
            return;
        }
        throw new AccessDeniedException("You cannot access attendance records.");
    }

    private void authorizeWrite(AttendanceRecord record, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            Schedule schedule = getSchedule(record.getScheduleId());
            if (!teacher.getTeacherId().equals(schedule.getTeacherId())) {
                throw new AccessDeniedException("Teachers can only edit attendance for their own schedules.");
            }
            return;
        }
        throw new AccessDeniedException("Only an administrator or the scheduled teacher can edit attendance.");
    }

    private Teacher getAuthorizedMarker(Schedule schedule, Integer requesterId, User.Role requesterRole) {
        Teacher scheduleTeacher = getActiveTeacher(schedule.getTeacherId());
        if (requesterRole == User.Role.Admin) {
            return scheduleTeacher;
        }
        if (requesterRole != User.Role.Teacher) {
            throw new AccessDeniedException("Only an administrator or the scheduled teacher can record attendance.");
        }
        Teacher requester = getActiveTeacherForUser(requesterId);
        if (!requester.getTeacherId().equals(schedule.getTeacherId())) {
            throw new AccessDeniedException("Teachers can only record attendance for their own schedules.");
        }
        return requester;
    }

    private void validateStudentEnrollment(Integer studentId, Integer classId, LocalDate attendanceDate) {
        if (!enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndEnrollmentDateLessThanEqual(
            studentId, classId, "Active", attendanceDate
        )) {
            throw new AttendanceRecordConflictException(
                "The student must be actively enrolled in the class on the attendance date."
            );
        }
    }

    private void validateAttendanceDate(Schedule schedule, LocalDate attendanceDate) {
        SchoolClass schoolClass = schoolClassRepository.findById(schedule.getClassId())
            .orElseThrow(() -> new SchoolClassNotFoundException(schedule.getClassId()));
        Semester semester = semesterRepository.findById(schoolClass.getSemesterId())
            .orElseThrow(() -> new SemesterNotFoundException(schoolClass.getSemesterId()));
        if (attendanceDate.isBefore(semester.getStartDate()) || attendanceDate.isAfter(semester.getEndDate())) {
            throw new AttendanceRecordConflictException("Attendance date must fall within the class semester.");
        }
    }

    public void delete(Integer attendanceId) {
        getAttendanceRecord(attendanceId);
        attendanceRepository.deleteById(attendanceId);
    }

    private AttendanceRecord getAttendanceRecord(Integer attendanceId) {
        return attendanceRepository.findById(attendanceId)
            .orElseThrow(() -> new AttendanceRecordNotFoundException(attendanceId));
    }

    private Schedule getSchedule(Integer scheduleId) {
        return scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    private Student getStudent(Integer studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
            .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private Teacher getActiveTeacherForUser(Integer userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
            .orElseThrow(() -> new AccessDeniedException("No teacher profile is associated with this account."));
        if (!"Active".equals(teacher.getStatus())) {
            throw new AccessDeniedException("This teacher profile is inactive.");
        }
        return teacher;
    }

    private Teacher getActiveTeacher(Integer teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new TeacherNotFoundException(teacherId));
        if (!"Active".equals(teacher.getStatus())) {
            throw new AttendanceRecordConflictException("Attendance cannot be marked by an inactive teacher.");
        }
        return teacher;
    }
}
