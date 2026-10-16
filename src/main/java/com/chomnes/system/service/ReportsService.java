package com.chomnes.system.service;

import com.chomnes.system.model.*;
import com.chomnes.system.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportsService {
    private static final Locale ENGLISH = Locale.US;
    private static final List<String> DAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final UserRepository users;
    private final SchoolClassRepository classes;
    private final ClassEnrollmentRepository enrollments;
    private final ScheduleRepository schedules;
    private final TimeSlotRepository timeSlots;
    private final AttendanceRecordRepository attendance;
    private final FinalGradeRepository finalGrades;
    private final SubjectRepository subjects;
    private final AcademicYearRepository academicYears;
    private final SemesterRepository semesters;

    public ReportsService(
            PaymentRepository payments,
            InvoiceRepository invoices,
            StudentRepository students,
            TeacherRepository teachers,
            UserRepository users,
            SchoolClassRepository classes,
            ClassEnrollmentRepository enrollments,
            ScheduleRepository schedules,
            TimeSlotRepository timeSlots,
            AttendanceRecordRepository attendance,
            FinalGradeRepository finalGrades,
            SubjectRepository subjects,
            AcademicYearRepository academicYears,
            SemesterRepository semesters
    ) {
        this.payments = payments;
        this.invoices = invoices;
        this.students = students;
        this.teachers = teachers;
        this.users = users;
        this.classes = classes;
        this.enrollments = enrollments;
        this.schedules = schedules;
        this.timeSlots = timeSlots;
        this.attendance = attendance;
        this.finalGrades = finalGrades;
        this.subjects = subjects;
        this.academicYears = academicYears;
        this.semesters = semesters;
    }

    public Map<String, Object> summary(String requestedRange, Integer academicYearId, Integer semesterId) {
        String range = normalizeRange(requestedRange);
        List<Payment> paymentRows = payments.findAllByOrderByPaymentDateDescPaymentIdDesc();
        List<Invoice> invoiceRows = invoices.findAll();
        List<Student> studentRows = students.findAll();
        List<Teacher> teacherRows = teachers.findAll();
        List<SchoolClass> classRows = classes.findAll();
        List<ClassEnrollment> enrollmentRows = enrollments.findAll();
        List<Schedule> scheduleRows = schedules.findAll();
        List<AttendanceRecord> attendanceRows = attendance.findAll();
        List<FinalGrade> finalGradeRows = finalGrades.findAll();
        Map<Integer, Invoice> invoiceById = index(invoiceRows, Invoice::getInvoiceId);
        Map<Integer, SchoolClass> classById = index(classRows, SchoolClass::getClassId);
        Map<Integer, Schedule> scheduleById = index(scheduleRows, Schedule::getScheduleId);
        Map<Integer, TimeSlot> timeSlotById = index(timeSlots.findAll(), TimeSlot::getTimeSlotId);
        Map<Integer, Subject> subjectById = index(subjects.findAll(), Subject::getSubjectId);
        Map<Integer, Student> studentById = index(studentRows, Student::getStudentId);
        Map<Integer, AcademicYear> academicYearById = index(academicYears.findAll(), AcademicYear::getAcademicYearId);
        Map<Integer, Semester> semesterById = index(semesters.findAll(), Semester::getSemesterId);

        return object(
                "financial", financial(paymentRows, invoiceRows, invoiceById, studentById, range, semesterId),
                "enrollment", enrollment(studentRows, classRows, enrollmentRows, academicYearById, semesterById,
                        academicYearId, semesterId),
                "academic", academic(attendanceRows, scheduleById, classById, finalGradeRows,
                        subjectById, semesterId, academicYearId),
                "teacherWorkload", teacherWorkload(teacherRows, classRows, scheduleRows,
                        timeSlotById, academicYearId, semesterId)
        );
    }

    private Map<String, Object> financial(
            List<Payment> paymentRows,
            List<Invoice> invoiceRows,
            Map<Integer, Invoice> invoiceById,
            Map<Integer, Student> studentById,
            String range,
            Integer semesterId
    ) {
        List<Invoice> selectedInvoices = invoiceRows.stream()
                .filter(invoice -> semesterId == null || semesterId.equals(invoice.getSemesterId())).toList();
        List<Payment> selectedPayments = paymentRows.stream().filter(payment -> {
            if (semesterId == null) return true;
            Invoice invoice = invoiceById.get(payment.getInvoiceId());
            return invoice != null && semesterId.equals(invoice.getSemesterId());
        }).toList();

        BigDecimal revenue = sumPayments(selectedPayments);
        BigDecimal billed = selectedInvoices.stream().map(Invoice::getTotalAmount).map(ReportsService::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = selectedInvoices.stream().map(Invoice::getAmountPaid).map(ReportsService::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Invoice> outstanding = selectedInvoices.stream().filter(this::isOutstanding)
                .sorted(Comparator.comparing(Invoice::getDueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<Map<String, Object>> outstandingRows = outstanding.stream().limit(50).map(invoice -> {
            Student student = studentById.get(invoice.getStudentId());
            BigDecimal total = amount(invoice.getTotalAmount());
            BigDecimal amountPaid = amount(invoice.getAmountPaid());
            return object("invoiceId", invoice.getInvoiceId(), "invoiceNumber", invoice.getInvoiceNumber(),
                    "studentName", student == null ? "—" : fullName(student.getFirstName(), student.getLastName()),
                    "issueDate", invoice.getIssueDate(), "dueDate", invoice.getDueDate(),
                    "totalAmount", total, "amountPaid", amountPaid, "balanceDue", total.subtract(amountPaid),
                    "status", invoice.getStatus());
        }).toList();
        BigDecimal outstandingAmount = billed.subtract(paid);
        BigDecimal collectionRate = billed.signum() == 0 ? BigDecimal.ZERO
                : paid.multiply(BigDecimal.valueOf(100)).divide(billed, 1, RoundingMode.HALF_UP);

        Map<String, BigDecimal> methods = new TreeMap<>();
        selectedPayments.forEach(payment -> methods.merge(
                payment.getPaymentMethod() == null ? "Other" : payment.getPaymentMethod(),
                amount(payment.getAmount()), BigDecimal::add));
        List<Map<String, Object>> methodRows = methods.entrySet().stream()
                .map(entry -> object("name", entry.getKey(), "value", entry.getValue())).toList();

        return object("totalRevenue", revenue, "totalRevenueFormatted", currency(revenue),
                "totalOutstanding", outstandingAmount, "totalOutstandingFormatted", currency(outstandingAmount),
                "collectionRate", collectionRate, "outstandingInvoicesCount", outstanding.size(),
                "revenueTrend", revenueTrend(selectedPayments, range), "paymentMethods", methodRows,
                "outstandingInvoices", object("totalCount", outstanding.size(), "totalAmount", outstandingAmount,
                        "totalAmountFormatted", currency(outstandingAmount), "rows", outstandingRows));
    }

    private Map<String, Object> enrollment(
            List<Student> studentRows,
            List<SchoolClass> classRows,
            List<ClassEnrollment> enrollmentRows,
            Map<Integer, AcademicYear> academicYearById,
            Map<Integer, Semester> semesterById,
            Integer academicYearId,
            Integer semesterId
    ) {
        long active = studentRows.stream().filter(student -> "Active".equals(student.getStatus())).count();
        long inactive = studentRows.stream().filter(student -> "Inactive".equals(student.getStatus())).count();
        Map<String, Long> statusCounts = studentRows.stream().collect(Collectors.groupingBy(
                student -> student.getStatus() == null ? "Unknown" : student.getStatus(), TreeMap::new, Collectors.counting()));
        Map<String, Long> genderCounts = studentRows.stream().filter(student -> student.getGender() == null || !student.getGender().isBlank())
                .collect(Collectors.groupingBy(student -> student.getGender() == null ? "Unknown" : student.getGender(),
                        TreeMap::new, Collectors.counting()));
        List<Map<String, Object>> capacity = classRows.stream()
                .filter(schoolClass -> (semesterId == null || semesterId.equals(schoolClass.getSemesterId()))
                        && (academicYearId == null || academicYearId.equals(schoolClass.getAcademicYearId())))
                .sorted(Comparator.comparing(SchoolClass::getClassId))
                .map(schoolClass -> {
                    long enrolled = enrollmentRows.stream().filter(row -> row.getClassId().equals(schoolClass.getClassId())
                            && "Active".equals(row.getStatus())).count();
                    int maximum = schoolClass.getMaxCapacity() == null ? 0 : schoolClass.getMaxCapacity();
                    AcademicYear year = academicYearById.get(schoolClass.getAcademicYearId());
                    Semester semester = semesterById.get(schoolClass.getSemesterId());
                    BigDecimal utilization = maximum > 0
                            ? BigDecimal.valueOf(enrolled * 100.0 / maximum).setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return object("classId", schoolClass.getClassId(), "className", schoolClass.getClassName(),
                            "academicYearName", year == null ? "—" : year.getYearName(),
                            "semesterName", semester == null ? "—" : semester.getSemesterName(),
                            "maxCapacity", maximum, "enrolled", enrolled,
                            "available", maximum > 0 ? maximum - enrolled : 0,
                            "utilizationPercent", utilization);
                }).toList();
        BigDecimal averageUtilization = capacity.isEmpty() ? BigDecimal.ZERO : capacity.stream()
                .map(row -> (BigDecimal) row.get("utilizationPercent")).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(capacity.size()), 1, RoundingMode.HALF_UP);
        return object("totalStudents", studentRows.size(), "activeStudents", active, "inactiveStudents", inactive,
                "newStudentsTrend", newStudentsTrend(studentRows),
                "statusDistribution", distribution(statusCounts), "genderDistribution", distribution(genderCounts),
                "classCapacity", capacity, "averageUtilization", averageUtilization);
    }

    private Map<String, Object> academic(
            List<AttendanceRecord> attendanceRows,
            Map<Integer, Schedule> scheduleById,
            Map<Integer, SchoolClass> classById,
            List<FinalGrade> allFinalGrades,
            Map<Integer, Subject> subjectById,
            Integer semesterId,
            Integer academicYearId
    ) {
        Map<String, Long> allAttendanceCounts = attendanceRows.stream().collect(Collectors.groupingBy(
                AttendanceRecord::getStatus, Collectors.counting()));
        long totalAttendance = attendanceRows.size();
        long present = allAttendanceCounts.getOrDefault("Present", 0L);
        long absent = allAttendanceCounts.getOrDefault("Absent", 0L);
        BigDecimal presentRate = percent(present, totalAttendance, 1);
        BigDecimal absentRate = percent(absent, totalAttendance, 1);

        Map<Integer, SchoolClass> selectedClasses = classById.entrySet().stream()
                .filter(entry -> (semesterId == null || semesterId.equals(entry.getValue().getSemesterId()))
                        && (academicYearId == null || academicYearId.equals(entry.getValue().getAcademicYearId())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Long> byClassAndStatus = new TreeMap<>();
        for (AttendanceRecord record : attendanceRows) {
            Schedule schedule = scheduleById.get(record.getScheduleId());
            SchoolClass schoolClass = schedule == null ? null : selectedClasses.get(schedule.getClassId());
            if (schoolClass == null) continue;
            String key = schoolClass.getClassId() + "|" + record.getStatus();
            byClassAndStatus.merge(key, 1L, Long::sum);
        }
        Map<Integer, Map<String, Object>> attendanceByClass = new TreeMap<>();
        byClassAndStatus.forEach((key, count) -> {
            String[] parts = key.split("\\|", 2);
            Integer id = Integer.valueOf(parts[0]);
            String status = parts[1];
            SchoolClass schoolClass = selectedClasses.get(id);
            Map<String, Object> row = attendanceByClass.computeIfAbsent(id, ignored -> object(
                    "classId", id, "className", schoolClass == null ? "—" : schoolClass.getClassName(),
                    "Present", 0L, "Absent", 0L, "Late", 0L, "Excused", 0L, "total", 0L));
            if (row.containsKey(status)) row.put(status, count);
            row.put("total", ((Number) row.get("total")).longValue() + count);
        });
        List<Map<String, Object>> attendanceRowsByClass = attendanceByClass.values().stream().map(row -> {
            long total = ((Number) row.get("total")).longValue();
            row.put("presentRate", percent(((Number) row.get("Present")).longValue(), total, 1));
            return row;
        }).toList();

        List<FinalGrade> filteredGrades = allFinalGrades.stream()
                .filter(grade -> semesterId == null || semesterId.equals(grade.getSemesterId())).toList();
        Map<String, GradeGroup> groups = new TreeMap<>();
        for (FinalGrade grade : filteredGrades) {
            SchoolClass schoolClass = classById.get(grade.getClassId());
            Subject subject = subjectById.get(grade.getSubjectId());
            if (schoolClass == null || subject == null) continue;
            String key = String.format("%010d|%010d", grade.getClassId(), grade.getSubjectId());
            GradeGroup group = groups.computeIfAbsent(key, ignored -> new GradeGroup(schoolClass, subject));
            group.add(grade.getFinalScore());
        }
        List<Map<String, Object>> gradesByClassSubject = groups.values().stream().map(group -> object(
                "classId", group.schoolClass.getClassId(), "className", group.schoolClass.getClassName(),
                "subjectId", group.subject.getSubjectId(), "subjectName", group.subject.getSubjectName(),
                "averageScore", group.average(2), "studentCount", group.count
        )).toList();
        BigDecimal averageFinal = gradesByClassSubject.isEmpty() ? BigDecimal.ZERO : gradesByClassSubject.stream()
                .map(row -> (BigDecimal) row.get("averageScore")).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(gradesByClassSubject.size()), 2, RoundingMode.HALF_UP);
        Map<String, Long> letterCounts = filteredGrades.stream().filter(grade -> grade.getLetterGrade() != null)
                .collect(Collectors.groupingBy(FinalGrade::getLetterGrade, Collectors.counting()));
        List<Map<String, Object>> finalGradeDistribution = List.of("A", "B", "C", "D", "F").stream()
                .map(letter -> object("name", letter, "value", letterCounts.getOrDefault(letter, 0L))).toList();
        return object("attendanceSummary", object("present", present, "absent", absent,
                        "late", allAttendanceCounts.getOrDefault("Late", 0L),
                        "excused", allAttendanceCounts.getOrDefault("Excused", 0L), "total", totalAttendance,
                        "presentRate", presentRate, "absentRate", absentRate),
                "attendanceByClass", attendanceRowsByClass,
                "gradesByClassSubject", gradesByClassSubject,
                "finalGradeDistribution", finalGradeDistribution,
                "averageFinalScore", averageFinal, "totalGradedRecords", filteredGrades.size());
    }

    private Map<String, Object> teacherWorkload(
            List<Teacher> teacherRows,
            List<SchoolClass> classRows,
            List<Schedule> scheduleRows,
            Map<Integer, TimeSlot> timeSlotById,
            Integer academicYearId,
            Integer semesterId
    ) {
        List<Teacher> activeTeachers = teacherRows.stream().filter(teacher -> users.findById(teacher.getUserId())
                .map(user -> user.getStatus() == User.Status.Active).orElse(false)).toList();
        List<SchoolClass> filteredClasses = classRows.stream()
                .filter(schoolClass -> (semesterId == null || semesterId.equals(schoolClass.getSemesterId()))
                        && (academicYearId == null || academicYearId.equals(schoolClass.getAcademicYearId()))).toList();
        Set<Integer> filteredClassIds = filteredClasses.stream().map(SchoolClass::getClassId).collect(Collectors.toSet());
        List<Schedule> filteredSchedules = scheduleRows.stream().filter(schedule -> filteredClassIds.contains(schedule.getClassId())).toList();
        List<Map<String, Object>> workload = new ArrayList<>();
        for (Teacher teacher : activeTeachers) {
            long homeroomCount = filteredClasses.stream()
                    .filter(schoolClass -> teacher.getTeacherId().equals(schoolClass.getHomeroomTeacherId())).count();
            List<Schedule> ownSchedules = filteredSchedules.stream()
                    .filter(schedule -> teacher.getTeacherId().equals(schedule.getTeacherId())).toList();
            long scheduledClasses = ownSchedules.stream().map(Schedule::getClassId).distinct().count();
            long uniqueSubjects = ownSchedules.stream().map(Schedule::getSubjectId).distinct().count();
            Map<String, Long> density = new LinkedHashMap<>();
            DAYS.forEach(day -> density.put(day, 0L));
            for (Schedule schedule : ownSchedules) {
                TimeSlot slot = timeSlotById.get(schedule.getTimeSlotId());
                if (slot != null && density.containsKey(slot.getDayOfWeek())) {
                    density.put(slot.getDayOfWeek(), density.get(slot.getDayOfWeek()) + 1);
                }
            }
            workload.add(object("teacherId", teacher.getTeacherId(),
                    "teacherName", fullName(teacher.getFirstName(), teacher.getLastName()),
                    "homeroomClasses", homeroomCount, "scheduledClasses", scheduledClasses,
                    "uniqueSubjects", uniqueSubjects, "totalWeeklySlots", ownSchedules.size(),
                    "dailySlotDensity", density));
        }
        BigDecimal averageClasses = meanLong(workload, "scheduledClasses", activeTeachers.size());
        BigDecimal averageSlots = meanLong(workload, "totalWeeklySlots", activeTeachers.size());
        BigDecimal averageSubjects = meanLong(workload, "uniqueSubjects", activeTeachers.size());
        return object("totalTeachers", activeTeachers.size(), "averageClassesPerTeacher", averageClasses,
                "averageScheduleSlotsPerTeacher", averageSlots, "averageSubjectsPerTeacher", averageSubjects,
                "workload", workload);
    }

    private Map<String, Object> revenueTrend(List<Payment> rows, String range) {
        LocalDate today = LocalDate.now();
        List<LocalDate> periods = new ArrayList<>();
        DateTimeFormatter labelFormat;
        if ("weekly".equals(range)) {
            for (int i = 6; i >= 0; i--) periods.add(today.minusDays(i));
            labelFormat = DateTimeFormatter.ofPattern("MMM d", ENGLISH);
        } else if ("yearly".equals(range)) {
            for (int i = 4; i >= 0; i--) periods.add(LocalDate.of(today.getYear() - i, 1, 1));
            labelFormat = DateTimeFormatter.ofPattern("yyyy", ENGLISH);
        } else {
            LocalDate first = today.withDayOfMonth(1);
            for (int i = 0; i < today.lengthOfMonth(); i++) periods.add(first.plusDays(i));
            labelFormat = DateTimeFormatter.ofPattern("MMM d", ENGLISH);
        }
        Map<String, BigDecimal> totals = new HashMap<>();
        LocalDate since = switch (range) {
            case "weekly" -> today.minusDays(6);
            case "yearly" -> LocalDate.of(today.getYear() - 4, 1, 1);
            default -> today.withDayOfMonth(1);
        };
        for (Payment payment : rows) {
            LocalDate date = payment.getPaymentDate();
            if (date == null || date.isBefore(since) || date.isAfter(today)) continue;
            String key = periodKey(date, range);
            totals.merge(key, amount(payment.getAmount()), BigDecimal::add);
        }
        List<String> labels = periods.stream().map(date -> date.format(labelFormat)).toList();
        List<BigDecimal> data = periods.stream().map(date -> totals.getOrDefault(periodKey(date, range), BigDecimal.ZERO)).toList();
        return object("labels", labels, "data", data);
    }

    private Map<String, Object> newStudentsTrend(List<Student> rows) {
        LocalDate today = LocalDate.now();
        LocalDate first = today.withDayOfMonth(1).minusMonths(5);
        Map<String, Long> counts = rows.stream().filter(student -> student.getEnrollmentDate() != null
                        && !student.getEnrollmentDate().isBefore(first))
                .collect(Collectors.groupingBy(student -> student.getEnrollmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()));
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.withDayOfMonth(1).minusMonths(i);
            labels.add(month.format(DateTimeFormatter.ofPattern("MMM yyyy", ENGLISH)));
            data.add(counts.getOrDefault(month.format(DateTimeFormatter.ofPattern("yyyy-MM")), 0L));
        }
        return object("labels", labels, "data", data);
    }

    private List<Map<String, Object>> distribution(Map<String, Long> counts) {
        return counts.entrySet().stream().map(entry -> object("name", entry.getKey(), "value", entry.getValue())).toList();
    }

    private String normalizeRange(String range) {
        if (range == null) return "monthly";
        return switch (range.toLowerCase(Locale.ROOT)) {
            case "weekly", "yearly" -> range.toLowerCase(Locale.ROOT);
            default -> "monthly";
        };
    }

    private String periodKey(LocalDate date, String range) {
        return "yearly".equals(range) ? Integer.toString(date.getYear()) : date.toString();
    }

    private boolean isOutstanding(Invoice invoice) {
        return invoice.getStatus() != null && Set.of("Unpaid", "Partial", "Overdue").contains(invoice.getStatus());
    }

    private BigDecimal sumPayments(List<Payment> rows) {
        return rows.stream().map(Payment::getAmount).map(ReportsService::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal meanLong(List<Map<String, Object>> rows, String key, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        long sum = rows.stream().map(row -> ((Number) row.get(key)).longValue()).mapToLong(Long::longValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(long value, long total, int scale) {
        return total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal amount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static String currency(BigDecimal amount) {
        return "$" + amount(amount).setScale(2, RoundingMode.HALF_UP).toPlainString()
                .replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",");
    }

    private static String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private static <T> Map<Integer, T> index(List<T> rows, Function<T, Integer> key) {
        return rows.stream().collect(Collectors.toMap(key, Function.identity(), (first, ignored) -> first));
    }

    private static Map<String, Object> object(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static final class GradeGroup {
        private final SchoolClass schoolClass;
        private final Subject subject;
        private BigDecimal score = BigDecimal.ZERO;
        private long count;

        private GradeGroup(SchoolClass schoolClass, Subject subject) {
            this.schoolClass = schoolClass;
            this.subject = subject;
        }

        private void add(BigDecimal value) {
            score = score.add(amount(value));
            count++;
        }

        private BigDecimal average(int scale) {
            return count == 0 ? BigDecimal.ZERO : score.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP);
        }
    }
}
