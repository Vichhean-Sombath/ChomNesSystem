package com.chomnes.system.service;

import com.chomnes.system.dto.CreateSemesterRequest;
import com.chomnes.system.dto.SemesterResponse;
import com.chomnes.system.dto.UpdateSemesterRequest;
import com.chomnes.system.exception.AcademicYearNotFoundException;
import com.chomnes.system.exception.SemesterConflictException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.model.AcademicYear;
import com.chomnes.system.model.Semester;
import com.chomnes.system.repository.AcademicYearRepository;
import com.chomnes.system.repository.SemesterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SemesterService {
    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;

    public SemesterService(
        SemesterRepository semesterRepository,
        AcademicYearRepository academicYearRepository
    ) {
        this.semesterRepository = semesterRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional(readOnly = true)
    public List<SemesterResponse> findAll() {
        return semesterRepository.findAll(Sort.by(
                Sort.Order.asc("academicYearId"),
                Sort.Order.asc("startDate"),
                Sort.Order.asc("semesterId")
            ))
            .stream()
            .map(SemesterResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SemesterResponse findById(Integer semesterId) {
        return SemesterResponse.from(getSemester(semesterId));
    }

    public SemesterResponse create(CreateSemesterRequest request) {
        String semesterName = requireNonBlank(request.semesterName(), "Semester name");
        AcademicYear academicYear = getAcademicYear(request.academicYearId());
        requireActiveAcademicYear(academicYear);
        requireWithinAcademicYear(academicYear, request.startDate(), request.endDate());

        Semester semester = new Semester(
            academicYear.getAcademicYearId(),
            semesterName,
            request.startDate(),
            request.endDate()
        );
        return SemesterResponse.from(semesterRepository.save(semester));
    }

    public SemesterResponse update(Integer semesterId, UpdateSemesterRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one semester field to update.");
        }
        Semester semester = getSemester(semesterId);
        Integer academicYearId = request.academicYearId() == null
            ? semester.getAcademicYearId()
            : request.academicYearId();
        AcademicYear academicYear = getAcademicYear(academicYearId);
        requireActiveAcademicYear(academicYear);

        String semesterName = request.semesterName() == null
            ? semester.getSemesterName()
            : requireNonBlank(request.semesterName(), "Semester name");
        LocalDate startDate = request.startDate() == null ? semester.getStartDate() : request.startDate();
        LocalDate endDate = request.endDate() == null ? semester.getEndDate() : request.endDate();
        requireWithinAcademicYear(academicYear, startDate, endDate);

        semester.setAcademicYearId(academicYear.getAcademicYearId());
        semester.setSemesterName(semesterName);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        return SemesterResponse.from(semesterRepository.save(semester));
    }

    public void delete(Integer semesterId) {
        getSemester(semesterId);
        semesterRepository.deleteById(semesterId);
    }

    private Semester getSemester(Integer semesterId) {
        return semesterRepository.findById(semesterId)
            .orElseThrow(() -> new SemesterNotFoundException(semesterId));
    }

    private AcademicYear getAcademicYear(Integer academicYearId) {
        return academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new AcademicYearNotFoundException(academicYearId));
    }

    private String requireNonBlank(String value, String field) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return trimmed;
    }

    private void requireActiveAcademicYear(AcademicYear academicYear) {
        if (!"Active".equals(academicYear.getStatus())) {
            throw new SemesterConflictException("Semesters can only be added to an active academic year.");
        }
    }

    private void requireWithinAcademicYear(AcademicYear academicYear, LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        if (startDate.isBefore(academicYear.getStartDate()) || endDate.isAfter(academicYear.getEndDate())) {
            throw new IllegalArgumentException("Semester dates must fall within the academic year dates.");
        }
    }
}
