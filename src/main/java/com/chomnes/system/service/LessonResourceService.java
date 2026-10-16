package com.chomnes.system.service;

import com.chomnes.system.dto.CreateLessonResourceRequest;
import com.chomnes.system.dto.LessonResourceResponse;
import com.chomnes.system.dto.UpdateLessonResourceRequest;
import com.chomnes.system.exception.LessonResourceConflictException;
import com.chomnes.system.exception.LessonResourceNotFoundException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.exception.TeacherNotFoundException;
import com.chomnes.system.model.ClassEnrollment;
import com.chomnes.system.model.LessonResource;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.LessonResourceRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LessonResourceService {
    private final LessonResourceRepository resourceRepository;
    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public LessonResourceService(
            LessonResourceRepository resourceRepository,
            ScheduleRepository scheduleRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            ClassEnrollmentRepository enrollmentRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.scheduleRepository = scheduleRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<LessonResourceResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return resourceRepository.findAllByOrderByUploadDateDescResourceIdDesc().stream()
                    .map(LessonResourceResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            return resourceRepository.findAllByTeacherIdOrderByUploadDateDescResourceIdDesc(teacher.getTeacherId())
                    .stream()
                    .map(LessonResourceResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            List<Integer> classIds = enrollmentRepository
                    .findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(student.getStudentId()).stream()
                    .filter(enrollment -> "Active".equals(enrollment.getStatus()))
                    .map(ClassEnrollment::getClassId)
                    .distinct()
                    .toList();
            if (classIds.isEmpty()) {
                return List.of();
            }
            List<Integer> scheduleIds = scheduleRepository.findAllByClassIdIn(classIds).stream()
                    .map(Schedule::getScheduleId)
                    .toList();
            if (scheduleIds.isEmpty()) {
                return List.of();
            }
            return resourceRepository.findAllByScheduleIdInOrderByUploadDateDescResourceIdDesc(scheduleIds).stream()
                    .map(LessonResourceResponse::from)
                    .toList();
        }
        throw new AccessDeniedException("You cannot access lesson resources.");
    }

    @Transactional(readOnly = true)
    public LessonResourceResponse findById(Integer resourceId, Integer requesterId, User.Role requesterRole) {
        LessonResource resource = getResource(resourceId);
        authorizeRead(resource, requesterId, requesterRole);
        return LessonResourceResponse.from(resource);
    }

    public LessonResourceResponse create(
            CreateLessonResourceRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        Teacher teacher = resolveOwner(request.teacherId(), requesterId, requesterRole);
        validateSchedule(request.scheduleId(), teacher.getTeacherId());
        LessonResource resource = new LessonResource(
                request.scheduleId(),
                teacher.getTeacherId(),
                requireNonBlank(request.title()),
                request.description(),
                request.resourceType(),
                request.fileUrl()
        );
        return LessonResourceResponse.from(resourceRepository.save(resource));
    }

    public LessonResourceResponse update(
            Integer resourceId,
            UpdateLessonResourceRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one lesson resource field to update.");
        }
        ensureClearFlagsAreExclusive(request);
        LessonResource resource = getResource(resourceId);
        authorizeManage(resource, requesterId, requesterRole);

        Integer scheduleId = Boolean.TRUE.equals(request.clearSchedule())
                ? null
                : request.scheduleId() == null ? resource.getScheduleId() : request.scheduleId();
        validateSchedule(scheduleId, resource.getTeacherId());
        resource.setScheduleId(scheduleId);
        if (request.title() != null) {
            resource.setTitle(requireNonBlank(request.title()));
        }
        if (Boolean.TRUE.equals(request.clearDescription())) {
            resource.setDescription(null);
        } else if (request.description() != null) {
            resource.setDescription(request.description());
        }
        if (request.resourceType() != null) {
            resource.setResourceType(request.resourceType());
        }
        if (Boolean.TRUE.equals(request.clearFileUrl())) {
            resource.setFileUrl(null);
        } else if (request.fileUrl() != null) {
            resource.setFileUrl(request.fileUrl());
        }
        return LessonResourceResponse.from(resourceRepository.save(resource));
    }

    public void delete(Integer resourceId, Integer requesterId, User.Role requesterRole) {
        LessonResource resource = getResource(resourceId);
        authorizeManage(resource, requesterId, requesterRole);
        resourceRepository.delete(resource);
    }

    @Transactional(readOnly = true)
    public boolean canReadFile(String fileUrl, Integer requesterId, User.Role requesterRole) {
        LessonResource resource = resourceRepository.findByFileUrl(fileUrl).orElse(null);
        if (resource == null) return false;
        try {
            authorizeRead(resource, requesterId, requesterRole);
            return true;
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    public void attachFile(Integer resourceId, String fileUrl, Integer requesterId, User.Role requesterRole) {
        LessonResource resource = getResource(resourceId);
        authorizeManage(resource, requesterId, requesterRole);
        resource.setFileUrl(fileUrl);
        resourceRepository.save(resource);
    }

    private LessonResource getResource(Integer resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new LessonResourceNotFoundException(resourceId));
    }

    private Teacher resolveOwner(Integer requestedTeacherId, Integer requesterId, User.Role requesterRole) {
        Teacher teacher;
        if (requesterRole == User.Role.Admin) {
            if (requestedTeacherId == null) {
                throw new IllegalArgumentException("teacherId is required when an administrator creates a resource.");
            }
            teacher = teacherRepository.findById(requestedTeacherId)
                    .orElseThrow(() -> new TeacherNotFoundException(requestedTeacherId));
        } else if (requesterRole == User.Role.Teacher) {
            teacher = getActiveTeacherForUser(requesterId);
            if (requestedTeacherId != null && !teacher.getTeacherId().equals(requestedTeacherId)) {
                throw new AccessDeniedException("Teachers can only create resources under their own profile.");
            }
        } else {
            throw new AccessDeniedException("Only administrators and teachers can manage lesson resources.");
        }
        if (!"Active".equals(teacher.getStatus())) {
            throw new AccessDeniedException("Resources cannot be assigned to an inactive teacher.");
        }
        return teacher;
    }

    private void validateSchedule(Integer scheduleId, Integer teacherId) {
        if (scheduleId == null) {
            return;
        }
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        if (!teacherId.equals(schedule.getTeacherId())) {
            throw new LessonResourceConflictException("The selected schedule does not belong to this resource's teacher.");
        }
    }

    private void authorizeRead(LessonResource resource, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            if (!teacher.getTeacherId().equals(resource.getTeacherId())) {
                throw new AccessDeniedException("Teachers can only access their own lesson resources.");
            }
            return;
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (resource.getScheduleId() == null) {
                throw new AccessDeniedException("This lesson resource is not assigned to an enrolled schedule.");
            }
            Schedule schedule = scheduleRepository.findById(resource.getScheduleId())
                    .orElseThrow(() -> new ScheduleNotFoundException(resource.getScheduleId()));
            if (!enrollmentRepository.existsByStudentIdAndClassIdAndStatus(
                    student.getStudentId(), schedule.getClassId(), "Active"
            )) {
                throw new AccessDeniedException("Students can only access resources for their active classes.");
            }
            return;
        }
        throw new AccessDeniedException("You cannot access lesson resources.");
    }

    private void authorizeManage(LessonResource resource, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole != User.Role.Teacher) {
            throw new AccessDeniedException("Only administrators and teachers can manage lesson resources.");
        }
        Teacher teacher = getActiveTeacherForUser(requesterId);
        if (!teacher.getTeacherId().equals(resource.getTeacherId())) {
            throw new AccessDeniedException("Teachers can only manage their own lesson resources.");
        }
    }

    private void ensureClearFlagsAreExclusive(UpdateLessonResourceRequest request) {
        if (Boolean.TRUE.equals(request.clearSchedule()) && request.scheduleId() != null) {
            throw new IllegalArgumentException("Set either scheduleId or clearSchedule, not both.");
        }
        if (Boolean.TRUE.equals(request.clearDescription()) && request.description() != null) {
            throw new IllegalArgumentException("Set either description or clearDescription, not both.");
        }
        if (Boolean.TRUE.equals(request.clearFileUrl()) && request.fileUrl() != null) {
            throw new IllegalArgumentException("Set either fileUrl or clearFileUrl, not both.");
        }
    }

    private Teacher getActiveTeacherForUser(Integer userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No teacher profile is associated with this account."));
        if (!"Active".equals(teacher.getStatus())) {
            throw new AccessDeniedException("This teacher profile is inactive.");
        }
        return teacher;
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private String requireNonBlank(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Lesson resource title must not be blank.");
        }
        return trimmed;
    }
}
