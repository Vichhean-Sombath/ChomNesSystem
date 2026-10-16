package com.chomnes.system.service;

import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.TeacherRepository;
import com.chomnes.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AttendanceQrService {
    private static final String ISSUER = "chomnes-system";
    private static final String PURPOSE = "attendance-checkin";
    private final ScheduleRepository schedules;
    private final TeacherRepository teachers;
    private final UserRepository users;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long lifetimeSeconds;

    public AttendanceQrService(
            ScheduleRepository schedules,
            TeacherRepository teachers,
            UserRepository users,
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            @Value("${app.attendance.qr-ttl-seconds:120}") long lifetimeSeconds
    ) {
        this.schedules = schedules;
        this.teachers = teachers;
        this.users = users;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.lifetimeSeconds = lifetimeSeconds;
    }

    public Map<String, Object> issue(Integer scheduleId, Integer requesterId, User.Role requesterRole) {
        User account = users.findById(requesterId)
                .orElseThrow(() -> new AccessDeniedException("Active account required to issue an attendance QR."));
        if (account.getStatus() != User.Status.Active || account.getRole() != requesterRole) {
            throw new AccessDeniedException("Active account required to issue an attendance QR.");
        }
        Schedule schedule = schedules.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        authorizeSchedule(schedule, requesterId, requesterRole);
        if (lifetimeSeconds < 1 || lifetimeSeconds > 900) {
            throw new IllegalStateException("Attendance QR lifetime must be between 1 and 900 seconds.");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(lifetimeSeconds);
        LocalDate attendanceDate = LocalDate.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(PURPOSE)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("purpose", PURPOSE)
                .claim("schedule_id", schedule.getScheduleId())
                .claim("class_id", schedule.getClassId())
                .claim("attendance_date", attendanceDate.toString())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims
        )).getTokenValue();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("qrPayload", token);
        response.put("scheduleId", schedule.getScheduleId());
        response.put("attendanceDate", attendanceDate);
        response.put("issuedAt", issuedAt);
        response.put("expiresAt", expiresAt);
        response.put("expiresInSeconds", lifetimeSeconds);
        return response;
    }

    public void verify(String token, Integer scheduleId, Integer classId, LocalDate attendanceDate) {
        Jwt claims;
        try {
            claims = jwtDecoder.decode(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid or expired attendance QR token.");
        }
        if (!ISSUER.equals(claims.getIssuer() == null ? null : claims.getIssuer().toString())
                || !PURPOSE.equals(claims.getClaimAsString("purpose"))
                || !PURPOSE.equals(claims.getSubject())
                || !matchesId(claims, "schedule_id", scheduleId)
                || !matchesId(claims, "class_id", classId)
                || !attendanceDate.toString().equals(claims.getClaimAsString("attendance_date"))) {
            throw new AccessDeniedException("Attendance QR token does not match this schedule and date.");
        }
    }

    private boolean matchesId(Jwt claims, String name, Integer expected) {
        Object value = claims.getClaim(name);
        return value instanceof Number number && number.intValue() == expected;
    }

    private void authorizeSchedule(Schedule schedule, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) return;
        if (requesterRole != User.Role.Teacher) {
            throw new AccessDeniedException("Only an administrator or the scheduled teacher can issue an attendance QR.");
        }
        Teacher teacher = teachers.findByUserId(requesterId)
                .orElseThrow(() -> new AccessDeniedException("No teacher profile is associated with this account."));
        if (!"Active".equals(teacher.getStatus()) || !teacher.getTeacherId().equals(schedule.getTeacherId())) {
            throw new AccessDeniedException("Teachers can issue attendance QR tokens only for their own active schedules.");
        }
    }
}
