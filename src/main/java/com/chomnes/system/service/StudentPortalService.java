package com.chomnes.system.service;

import com.chomnes.system.dto.StudentAttendanceCheckInRequest;
import com.chomnes.system.exception.AttendanceRecordConflictException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.model.*;
import com.chomnes.system.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class StudentPortalService {
    private static final List<String> DAY_ORDER = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private final StudentRepository students;
    private final UserRepository users;
    private final ClassEnrollmentRepository enrollments;
    private final SchoolClassRepository classes;
    private final AcademicYearRepository academicYears;
    private final SemesterRepository semesters;
    private final TeacherRepository teachers;
    private final ScheduleRepository schedules;
    private final SubjectRepository subjects;
    private final TimeSlotRepository timeSlots;
    private final GradeRepository grades;
    private final AssessmentRepository assessments;
    private final FinalGradeRepository finalGrades;
    private final AttendanceRecordRepository attendanceRecords;
    private final InvoiceRepository invoices;
    private final FeeStructureRepository fees;
    private final PaymentRepository payments;
    private final CertificateRepository certificates;
    private final LessonResourceRepository resources;
    private final StudentEmergencyContactRepository emergencyContacts;
    private final String checkInToken;

    public StudentPortalService(
            StudentRepository students,
            UserRepository users,
            ClassEnrollmentRepository enrollments,
            SchoolClassRepository classes,
            AcademicYearRepository academicYears,
            SemesterRepository semesters,
            TeacherRepository teachers,
            ScheduleRepository schedules,
            SubjectRepository subjects,
            TimeSlotRepository timeSlots,
            GradeRepository grades,
            AssessmentRepository assessments,
            FinalGradeRepository finalGrades,
            AttendanceRecordRepository attendanceRecords,
            InvoiceRepository invoices,
            FeeStructureRepository fees,
            PaymentRepository payments,
            CertificateRepository certificates,
            LessonResourceRepository resources,
            StudentEmergencyContactRepository emergencyContacts,
            @Value("${app.attendance.checkin-token:}") String checkInToken
    ) {
        this.students = students;
        this.users = users;
        this.enrollments = enrollments;
        this.classes = classes;
        this.academicYears = academicYears;
        this.semesters = semesters;
        this.teachers = teachers;
        this.schedules = schedules;
        this.subjects = subjects;
        this.timeSlots = timeSlots;
        this.grades = grades;
        this.assessments = assessments;
        this.finalGrades = finalGrades;
        this.attendanceRecords = attendanceRecords;
        this.invoices = invoices;
        this.fees = fees;
        this.payments = payments;
        this.certificates = certificates;
        this.resources = resources;
        this.emergencyContacts = emergencyContacts;
        this.checkInToken = checkInToken;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> profile(Integer userId) {
        Student student = getStudent(userId);
        User user = users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("No active user is associated with this student profile."));
        List<Map<String, Object>> contacts = emergencyContacts
                .findAllByStudentIdOrderByContactNameAscContactIdAsc(student.getStudentId()).stream()
                .map(contact -> object(
                        "contactId", contact.getContactId(), "contactName", contact.getContactName(),
                        "relationship", contact.getRelationship(), "phoneNumber", contact.getPhoneNumber(),
                        "email", contact.getEmail()
                )).toList();
        return object(
                "studentId", student.getStudentId(), "firstName", student.getFirstName(),
                "lastName", student.getLastName(), "fullName", fullName(student.getFirstName(), student.getLastName()),
                "dob", student.getDob(), "gender", student.getGender(), "photoUrl", student.getPhotoUrl(),
                "contactNumber", student.getContactNumber(), "address", student.getAddress(),
                "enrollmentDate", student.getEnrollmentDate(), "status", student.getStatus(),
                "user", object("userId", user.getUserId(), "username", user.getUsername(),
                        "email", user.getEmail(), "status", user.getStatus(), "lastLoginAt", user.getLastLoginAt()),
                "emergencyContacts", contacts
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> classes(Integer userId) {
        Integer studentId = getStudent(userId).getStudentId();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassEnrollment enrollment : enrollments.findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(studentId)) {
            if (!"Active".equals(enrollment.getStatus())) continue;
            SchoolClass schoolClass = classes.findById(enrollment.getClassId()).orElse(null);
            if (schoolClass == null) continue;
            AcademicYear year = academicYears.findById(schoolClass.getAcademicYearId()).orElse(null);
            Semester semester = semesters.findById(schoolClass.getSemesterId()).orElse(null);
            Teacher homeroom = schoolClass.getHomeroomTeacherId() == null ? null
                    : teachers.findById(schoolClass.getHomeroomTeacherId()).orElse(null);
            List<Map<String, Object>> scheduleRows = schedules.findAllByClassIdIn(List.of(schoolClass.getClassId())).stream()
                    .map(this::scheduleDetails).toList();
            result.add(object(
                    "enrollmentId", enrollment.getEnrollmentId(), "enrollmentDate", enrollment.getEnrollmentDate(),
                    "status", enrollment.getStatus(), "classId", schoolClass.getClassId(),
                    "className", schoolClass.getClassName(), "roomNumber", schoolClass.getRoomNumber(),
                    "maxCapacity", schoolClass.getMaxCapacity(), "academicYear", year == null ? "—" : year.getYearName(),
                    "semester", semester == null ? "—" : semester.getSemesterName(),
                    "homeroomTeacher", homeroom == null ? "—" : fullName(homeroom.getFirstName(), homeroom.getLastName()),
                    "scheduleCount", scheduleRows.size(), "schedules", scheduleRows
            ));
        }
        result.sort(Comparator.comparing(row -> Objects.toString(row.get("className"), ""), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> schedule(Integer userId) {
        Integer studentId = getStudent(userId).getStudentId();
        List<Integer> classIds = activeClassIds(studentId);
        if (classIds.isEmpty()) return List.of();
        List<Map<String, Object>> result = schedules.findAllByClassIdIn(classIds).stream()
                .map(this::scheduleDetails).toList();
        result = new ArrayList<>(result);
        result.sort(Comparator
                .comparingInt((Map<String, Object> row) -> dayIndex(Objects.toString(row.get("dayOfWeek"), "")))
                .thenComparing(row -> Objects.toString(row.get("startTime"), "")));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> grades(Integer userId) {
        Integer studentId = getStudent(userId).getStudentId();
        List<Map<String, Object>> assessmentGrades = new ArrayList<>();
        for (Grade grade : grades.findAllByStudentIdAndIsPublishedTrueOrderByEnteredAtDescGradeIdDesc(studentId)) {
            Assessment assessment = assessments.findById(grade.getAssessmentId()).orElse(null);
            Schedule schedule = assessment == null ? null : schedules.findById(assessment.getScheduleId()).orElse(null);
            SchoolClass schoolClass = schedule == null ? null : classes.findById(schedule.getClassId()).orElse(null);
            Subject subject = schedule == null ? null : subjects.findById(schedule.getSubjectId()).orElse(null);
            Teacher marker = teachers.findById(grade.getEnteredBy()).orElse(null);
            BigDecimal maxScore = assessment == null ? BigDecimal.ZERO : assessment.getMaxScore();
            BigDecimal score = grade.getScore() == null ? BigDecimal.ZERO : grade.getScore();
            BigDecimal percentage = maxScore == null || maxScore.signum() == 0 ? BigDecimal.ZERO
                    : score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 1, RoundingMode.HALF_UP);
            assessmentGrades.add(object(
                    "gradeId", grade.getGradeId(), "assessmentId", assessment == null ? null : assessment.getAssessmentId(),
                    "assessmentName", assessment == null ? null : assessment.getAssessmentName(),
                    "maxScore", maxScore, "score", score, "percentage", percentage,
                    "assessmentDate", assessment == null ? null : assessment.getAssessmentDate(),
                    "subjectCode", subject == null ? null : subject.getSubjectCode(),
                    "subjectName", subject == null ? null : subject.getSubjectName(),
                    "className", schoolClass == null ? null : schoolClass.getClassName(),
                    "enteredBy", marker == null ? "—" : fullName(marker.getFirstName(), marker.getLastName()),
                    "enteredAt", grade.getEnteredAt()
            ));
        }
        List<Map<String, Object>> finalGradeRows = finalGrades.findAllByStudentIdOrderByComputedAtDescFinalGradeIdDesc(studentId).stream()
                .map(grade -> {
                    SchoolClass schoolClass = classes.findById(grade.getClassId()).orElse(null);
                    Subject subject = subjects.findById(grade.getSubjectId()).orElse(null);
                    Semester semester = semesters.findById(grade.getSemesterId()).orElse(null);
                    return object("finalGradeId", grade.getFinalGradeId(),
                            "className", schoolClass == null ? null : schoolClass.getClassName(),
                            "subjectCode", subject == null ? null : subject.getSubjectCode(),
                            "subjectName", subject == null ? null : subject.getSubjectName(),
                            "semesterName", semester == null ? null : semester.getSemesterName(),
                            "finalScore", grade.getFinalScore(), "letterGrade", grade.getLetterGrade(),
                            "gpaPoints", grade.getGpaPoints(), "computedAt", grade.getComputedAt() == null
                                    ? null : grade.getComputedAt().toLocalDate());
                }).toList();
        return object("assessmentGrades", assessmentGrades, "finalGrades", finalGradeRows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> attendance(Integer userId) {
        Integer studentId = getStudent(userId).getStudentId();
        int present = 0, absent = 0, late = 0, excused = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AttendanceRecord record : attendanceRecords.findAllByStudentIdOrderByAttendanceDateDescAttendanceIdDesc(studentId)) {
            switch (record.getStatus().toLowerCase(Locale.ROOT)) {
                case "present" -> present++;
                case "absent" -> absent++;
                case "late" -> late++;
                case "excused" -> excused++;
            }
            Schedule schedule = schedules.findById(record.getScheduleId()).orElse(null);
            SchoolClass schoolClass = schedule == null ? null : classes.findById(schedule.getClassId()).orElse(null);
            Subject subject = schedule == null ? null : subjects.findById(schedule.getSubjectId()).orElse(null);
            TimeSlot timeSlot = schedule == null ? null : timeSlots.findById(schedule.getTimeSlotId()).orElse(null);
            Teacher marker = teachers.findById(record.getMarkedBy()).orElse(null);
            rows.add(object(
                    "attendanceId", record.getAttendanceId(), "scheduleId", record.getScheduleId(),
                    "className", schoolClass == null ? null : schoolClass.getClassName(),
                    "subjectCode", subject == null ? null : subject.getSubjectCode(),
                    "subjectName", subject == null ? null : subject.getSubjectName(),
                    "dayOfWeek", timeSlot == null ? null : timeSlot.getDayOfWeek(),
                    "startTime", timeSlot == null ? null : timeSlot.getStartTime(),
                    "endTime", timeSlot == null ? null : timeSlot.getEndTime(),
                    "attendanceDate", record.getAttendanceDate(), "status", record.getStatus(),
                    "markedBy", marker == null ? "—" : fullName(marker.getFirstName(), marker.getLastName())
            ));
        }
        int total = rows.size();
        BigDecimal rate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(present * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP);
        return object("summary", object("present", present, "absent", absent, "late", late,
                "excused", excused, "total", total, "presentRate", rate), "records", rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> invoices(Integer userId) {
        Integer studentId = getStudent(userId).getStudentId();
        List<Invoice> invoiceRows = invoices.findAllByStudentIdOrderByCreatedAtDescInvoiceIdDesc(studentId);
        BigDecimal totalBilled = BigDecimal.ZERO, totalPaid = BigDecimal.ZERO;
        List<Map<String, Object>> results = new ArrayList<>();
        for (Invoice invoice : invoiceRows) {
            List<Payment> paymentRows = payments.findAllByInvoiceIdOrderByPaymentDateDescPaymentIdDesc(invoice.getInvoiceId());
            BigDecimal paid = paymentRows.stream().map(Payment::getAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
            BigDecimal balance = total.subtract(paid);
            totalBilled = totalBilled.add(total);
            totalPaid = totalPaid.add(paid);
            FeeStructure fee = fees.findById(invoice.getFeeId()).orElse(null);
            Semester semester = semesters.findById(invoice.getSemesterId()).orElse(null);
            List<Map<String, Object>> paymentItems = paymentRows.stream().map(p -> object(
                    "paymentId", p.getPaymentId(), "paymentDate", p.getPaymentDate(), "amount", p.getAmount(),
                    "method", p.getPaymentMethod(), "transactionReference", p.getTransactionReference(),
                    "receiptUrl", p.getReceiptUrl()
            )).toList();
            results.add(object("invoiceId", invoice.getInvoiceId(), "invoiceNumber", invoice.getInvoiceNumber(),
                    "feeName", fee == null ? "—" : fee.getFeeName(),
                    "semesterName", semester == null ? "—" : semester.getSemesterName(),
                    "issueDate", invoice.getIssueDate(), "dueDate", invoice.getDueDate(),
                    "totalAmount", total, "amountPaid", paid, "balanceDue", balance,
                    "status", balance.signum() <= 0 ? "Paid" : invoice.getStatus(), "payments", paymentItems));
        }
        BigDecimal outstanding = totalBilled.subtract(totalPaid);
        return object("summary", object("totalBilled", totalBilled, "totalPaid", totalPaid,
                "outstandingBalance", outstanding, "totalBilledFormatted", currency(totalBilled),
                "totalPaidFormatted", currency(totalPaid), "outstandingBalanceFormatted", currency(outstanding),
                "invoiceCount", invoiceRows.size()), "invoices", results);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> certificates(Integer userId) {
        Integer studentId = getStudent(userId).getStudentId();
        return certificates.findAllByStudentIdOrderByIssueDateDescCertificateIdDesc(studentId).stream()
                .map(certificate -> {
                    User issuer = users.findById(certificate.getIssuedBy()).orElse(null);
                    return object("certificateId", certificate.getCertificateId(),
                            "certificateType", certificate.getCertificateType(), "templateUsed", certificate.getTemplateUsed(),
                            "issueDate", certificate.getIssueDate(), "generatedFileUrl", certificate.getGeneratedFileUrl(),
                            "issuedBy", issuer == null ? "—" : issuer.getUsername());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> resources(Integer userId) {
        List<Integer> classIds = activeClassIds(getStudent(userId).getStudentId());
        if (classIds.isEmpty()) return List.of();
        List<Integer> scheduleIds = schedules.findAllByClassIdIn(classIds).stream().map(Schedule::getScheduleId).toList();
        if (scheduleIds.isEmpty()) return List.of();
        return resources.findAllByScheduleIdInOrderByUploadDateDescResourceIdDesc(scheduleIds).stream()
                .map(resource -> {
                    Schedule schedule = schedules.findById(resource.getScheduleId()).orElse(null);
                    SchoolClass schoolClass = schedule == null ? null : classes.findById(schedule.getClassId()).orElse(null);
                    Subject subject = schedule == null ? null : subjects.findById(schedule.getSubjectId()).orElse(null);
                    Teacher teacher = teachers.findById(resource.getTeacherId()).orElse(null);
                    return object("resourceId", resource.getResourceId(), "title", resource.getTitle(),
                            "description", resource.getDescription(), "resourceType", resource.getResourceType(),
                            "fileUrl", resource.getFileUrl(), "uploadDate", resource.getUploadDate() == null
                                    ? null : resource.getUploadDate().toLocalDate(),
                            "className", schoolClass == null ? null : schoolClass.getClassName(),
                            "subjectCode", subject == null ? null : subject.getSubjectCode(),
                            "subjectName", subject == null ? null : subject.getSubjectName(),
                            "teacherName", teacher == null ? "—" : fullName(teacher.getFirstName(), teacher.getLastName()));
                }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Integer userId) {
        Map<String, Object> profile = profile(userId);
        List<Map<String, Object>> classes = classes(userId);
        List<Map<String, Object>> schedule = schedule(userId);
        Map<String, Object> gradeData = grades(userId);
        Map<String, Object> attendanceData = attendance(userId);
        Map<String, Object> invoiceData = invoices(userId);
        List<Map<String, Object>> certificateRows = certificates(userId);
        List<Map<String, Object>> resourceRows = resources(userId);
        List<Map<String, Object>> assessmentGrades = castRows(gradeData.get("assessmentGrades"));
        List<Map<String, Object>> finalGradeRows = castRows(gradeData.get("finalGrades"));
        Map<String, Object> attendanceSummary = castMap(attendanceData.get("summary"));
        Map<String, Object> invoiceSummary = castMap(invoiceData.get("summary"));
        BigDecimal average = mean(assessmentGrades, "percentage", 1);
        BigDecimal averageFinal = mean(finalGradeRows, "finalScore", 2);
        List<Map<String, Object>> attendanceItems = castRows(attendanceData.get("records"));
        List<Map<String, Object>> invoiceItems = castRows(invoiceData.get("invoices"));
        Map<String, Object> profileSummary = object("fullName", profile.get("fullName"),
                "email", castMap(profile.get("user")).get("email"), "status", profile.get("status"));
        Map<String, Object> kpis = object(
                "classCount", classes.size(), "scheduleCount", schedule.size(),
                "publishedGradeCount", assessmentGrades.size(), "averageScore", average,
                "averageFinalScore", averageFinal, "attendancePresentRate", attendanceSummary.get("presentRate"),
                "attendanceTotal", attendanceSummary.get("total"),
                "outstandingBalance", invoiceSummary.get("outstandingBalance"),
                "outstandingBalanceFormatted", invoiceSummary.get("outstandingBalanceFormatted"),
                "invoiceCount", invoiceSummary.get("invoiceCount"), "certificateCount", certificateRows.size(),
                "resourceCount", resourceRows.size()
        );
        List<Map<String, Object>> upcoming = schedule.stream()
                .filter(row -> List.of("Mon", "Tue", "Wed", "Thu", "Fri").contains(row.get("dayOfWeek")))
                .limit(3).toList();
        return object("profile", profileSummary, "kpis", kpis,
                "recentGrades", assessmentGrades.stream().limit(5).toList(),
                "recentAttendance", attendanceItems.stream().limit(5).toList(),
                "upcomingSchedule", upcoming, "recentInvoices", invoiceItems.stream().limit(5).toList());
    }

    public Map<String, Object> checkIn(Integer userId, StudentAttendanceCheckInRequest request) {
        Student student = getStudent(userId);
        if (!"Active".equals(student.getStatus())) throw new AccessDeniedException("Inactive students cannot check in.");
        if (checkInToken == null || checkInToken.isBlank()) {
            throw new IllegalStateException("ATTENDANCE_CHECKIN_TOKEN is not configured.");
        }
        if (!checkInToken.equals(request.token())) throw new AccessDeniedException("Invalid check-in token.");
        if (!LocalDate.now().equals(request.attendanceDate())) {
            throw new IllegalArgumentException("Check-in is only allowed for today.");
        }
        Schedule schedule = schedules.findById(request.scheduleId())
                .orElseThrow(() -> new ScheduleNotFoundException(request.scheduleId()));
        if (!enrollments.existsByStudentIdAndClassIdAndStatusAndEnrollmentDateLessThanEqual(
                student.getStudentId(), schedule.getClassId(), "Active", request.attendanceDate())) {
            throw new AccessDeniedException("You are not actively enrolled in this class.");
        }
        if (attendanceRecords.existsByScheduleIdAndStudentIdAndAttendanceDate(
                schedule.getScheduleId(), student.getStudentId(), request.attendanceDate())) {
            throw new AttendanceRecordConflictException("Attendance already recorded for this schedule and date.");
        }
        AttendanceRecord record = attendanceRecords.save(new AttendanceRecord(
                schedule.getScheduleId(), student.getStudentId(), request.attendanceDate(), "Present", schedule.getTeacherId()
        ));
        return object("attendanceId", record.getAttendanceId(), "scheduleId", record.getScheduleId(),
                "studentId", record.getStudentId(), "attendanceDate", record.getAttendanceDate(), "status", record.getStatus());
    }

    private List<Integer> activeClassIds(Integer studentId) {
        return enrollments.findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(studentId).stream()
                .filter(enrollment -> "Active".equals(enrollment.getStatus()))
                .map(ClassEnrollment::getClassId).distinct().toList();
    }

    private Map<String, Object> scheduleDetails(Schedule schedule) {
        SchoolClass schoolClass = classes.findById(schedule.getClassId()).orElse(null);
        Subject subject = subjects.findById(schedule.getSubjectId()).orElse(null);
        Teacher teacher = teachers.findById(schedule.getTeacherId()).orElse(null);
        TimeSlot timeSlot = timeSlots.findById(schedule.getTimeSlotId()).orElse(null);
        return object("scheduleId", schedule.getScheduleId(), "classId", schedule.getClassId(),
                "className", schoolClass == null ? null : schoolClass.getClassName(),
                "subjectCode", subject == null ? null : subject.getSubjectCode(),
                "subjectName", subject == null ? null : subject.getSubjectName(),
                "teacherName", teacher == null ? "—" : fullName(teacher.getFirstName(), teacher.getLastName()),
                "dayOfWeek", timeSlot == null ? null : timeSlot.getDayOfWeek(),
                "startTime", timeSlot == null ? null : timeSlot.getStartTime(),
                "endTime", timeSlot == null ? null : timeSlot.getEndTime(), "roomNumber", schedule.getRoomNumber());
    }

    private Student getStudent(Integer userId) {
        User account = users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("No student account is associated with this identity."));
        if (account.getRole() != User.Role.Student || account.getStatus() != User.Status.Active) {
            throw new AccessDeniedException("An active student account is required.");
        }
        Student student = students.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
        if (!"Active".equals(student.getStatus())) throw new AccessDeniedException("This student profile is inactive.");
        return student;
    }

    private static String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private static int dayIndex(String day) {
        int index = DAY_ORDER.indexOf(day);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static String currency(BigDecimal value) {
        return "$" + (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP)
                .toPlainString().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",");
    }

    private static BigDecimal mean(List<Map<String, Object>> rows, String key, int scale) {
        BigDecimal sum = rows.stream().map(row -> row.get(key)).filter(Objects::nonNull)
                .map(value -> value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.isEmpty() ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(rows.size()), scale, RoundingMode.HALF_UP);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castRows(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private static Map<String, Object> object(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
