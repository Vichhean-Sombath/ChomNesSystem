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
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final Locale ENGLISH = Locale.US;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final SchoolClassRepository classes;

    public DashboardService(
            PaymentRepository payments,
            InvoiceRepository invoices,
            StudentRepository students,
            TeacherRepository teachers,
            SchoolClassRepository classes
    ) {
        this.payments = payments;
        this.invoices = invoices;
        this.students = students;
        this.teachers = teachers;
        this.classes = classes;
    }

    public Map<String, Object> summary(String range) {
        List<Payment> paymentRows = payments.findAllByOrderByPaymentDateDescPaymentIdDesc();
        List<Invoice> invoiceRows = invoices.findAll();
        List<Student> studentRows = students.findAll();
        List<Teacher> teacherRows = teachers.findAll();
        BigDecimal revenue = sum(paymentRows);
        return object(
                "kpis", object("totalRevenue", revenue, "totalRevenueFormatted", currency(revenue),
                        "totalStudents", studentRows.size(), "totalTeachers", teacherRows.size(),
                        "totalClasses", classes.count()),
                "revenueTrend", revenueTrend(paymentRows, normalizeRange(range)),
                "monthlyComparison", monthlyComparison(paymentRows),
                "paymentMethods", paymentMethods(paymentRows),
                "invoiceStatus", invoiceStatus(invoiceRows),
                "newStudentsTrend", studentTrend(studentRows),
                "recentPayments", recentPayments(paymentRows)
        );
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
        for (Payment payment : rows) {
            if (payment.getPaymentDate() == null) continue;
            String key = periodKey(payment.getPaymentDate(), range);
            totals.merge(key, payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount(), BigDecimal::add);
        }
        List<String> labels = periods.stream().map(d -> "yearly".equals(range)
                ? d.format(labelFormat) : d.format(labelFormat)).toList();
        List<BigDecimal> data = periods.stream().map(d -> totals.getOrDefault(periodKey(d, range), BigDecimal.ZERO)).toList();
        return object("labels", labels, "data", data);
    }

    private Map<String, Object> monthlyComparison(List<Payment> rows) {
        LocalDate today = LocalDate.now();
        LocalDate currentStart = today.withDayOfMonth(1);
        LocalDate previousStart = currentStart.minusMonths(1);
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal previous = BigDecimal.ZERO;
        for (Payment payment : rows) {
            LocalDate date = payment.getPaymentDate();
            if (date == null || payment.getAmount() == null) continue;
            if (!date.isBefore(currentStart)) current = current.add(payment.getAmount());
            else if (!date.isBefore(previousStart)) previous = previous.add(payment.getAmount());
        }
        BigDecimal difference = current.subtract(previous);
        BigDecimal percent = previous.signum() == 0
                ? current.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO
                : difference.multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP).abs();
        return object("current", current, "currentFormatted", currency(current), "previous", previous,
                "previousFormatted", currency(previous), "difference", difference,
                "differenceFormatted", currency(difference.abs()), "percent", percent,
                "direction", difference.signum() >= 0 ? "up" : "down");
    }

    private List<Map<String, Object>> paymentMethods(List<Payment> rows) {
        Map<String, BigDecimal> totals = rows.stream().collect(Collectors.groupingBy(
                payment -> payment.getPaymentMethod() == null ? "Other" : payment.getPaymentMethod(),
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO,
                        payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount(), BigDecimal::add)));
        return totals.entrySet().stream().map(entry -> object("name", entry.getKey(), "value", entry.getValue())).toList();
    }

    private List<Map<String, Object>> invoiceStatus(List<Invoice> rows) {
        Map<String, Long> counts = rows.stream().collect(Collectors.groupingBy(
                invoice -> invoice.getStatus() == null ? "Unknown" : invoice.getStatus(),
                LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream().map(entry -> object("name", entry.getKey(), "value", entry.getValue())).toList();
    }

    private Map<String, Object> studentTrend(List<Student> rows) {
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

    private List<Map<String, Object>> recentPayments(List<Payment> rows) {
        return rows.stream().limit(5).map(payment -> {
            Invoice invoice = payment.getInvoiceId() == null ? null : invoices.findById(payment.getInvoiceId()).orElse(null);
            return object("id", payment.getPaymentId(), "invoiceNumber", invoice == null ? "—" : invoice.getInvoiceNumber(),
                    "amount", payment.getAmount(), "method", payment.getPaymentMethod(), "date", payment.getPaymentDate());
        }).toList();
    }

    private BigDecimal sum(List<Payment> rows) {
        return rows.stream().map(Payment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeRange(String range) {
        if (range == null) return "monthly";
        return switch (range.toLowerCase(Locale.ROOT)) {
            case "weekly", "yearly" -> range.toLowerCase(Locale.ROOT);
            default -> "monthly";
        };
    }

    private String periodKey(LocalDate date, String range) {
        if ("yearly".equals(range)) return Integer.toString(date.getYear());
        if ("weekly".equals(range)) return date.toString();
        return date.toString();
    }

    private String currency(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                .replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",");
    }

    private static Map<String, Object> object(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
