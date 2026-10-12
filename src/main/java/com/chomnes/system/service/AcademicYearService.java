package com.chomnes.system.service;

import com.chomnes.system.dto.AcademicYearResponse;
import com.chomnes.system.dto.CreateAcademicYearRequest;
import com.chomnes.system.dto.UpdateAcademicYearRequest;
import com.chomnes.system.exception.AcademicYearConflictException;
import com.chomnes.system.exception.AcademicYearNotFoundException;
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
public class AcademicYearService {
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    public AcademicYearService(
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository
    ) {
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> findAll() {
        return academicYearRepository.findAll(Sort.by(Sort.Direction.ASC, "startDate"))
                .stream()
                .map(AcademicYearResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse findById(Integer academicYearId) {
        return AcademicYearResponse.from(getAcademicYear(academicYearId));
    }

    public AcademicYearResponse create(CreateAcademicYearRequest request) {
        String yearName = requireNonBlank(request.yearName(), "Year name");
        requireValidDates(request.startDate(), request.endDate());
        if (academicYearRepository.existsByYearName(yearName)) {
            throw new AcademicYearConflictException("An academic year with this name already exists.");
        }

        AcademicYear academicYear = new AcademicYear(yearName, request.startDate(), request.endDate());
        return AcademicYearResponse.from(academicYearRepository.save(academicYear));
    }

    public AcademicYearResponse update(Integer academicYearId, UpdateAcademicYearRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one academic year field to update.");
        }
        AcademicYear academicYear = getAcademicYear(academicYearId);

        String yearName = request.yearName() == null
                ? academicYear.getYearName()
                : requireNonBlank(request.yearName(), "Year name");
        LocalDate startDate = request.startDate() == null ? academicYear.getStartDate() : request.startDate();
        LocalDate endDate = request.endDate() == null ? academicYear.getEndDate() : request.endDate();
        requireValidDates(startDate, endDate);
        requireDatesContainSemesters(academicYearId, startDate, endDate);

        if (!yearName.equals(academicYear.getYearName())
                && academicYearRepository.existsByYearNameAndAcademicYearIdNot(yearName, academicYearId)) {
            throw new AcademicYearConflictException("An academic year with this name already exists.");
        }

        academicYear.setYearName(yearName);
        academicYear.setStartDate(startDate);
        academicYear.setEndDate(endDate);
        if (request.status() != null) {
            academicYear.setStatus(request.status());
        }
        return AcademicYearResponse.from(academicYearRepository.save(academicYear));
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

    private void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
    }

    private void requireDatesContainSemesters(Integer academicYearId, LocalDate startDate, LocalDate endDate) {
        for (Semester semester : semesterRepository.findAllByAcademicYearIdOrderByStartDateAsc(academicYearId)) {
            if (semester.getStartDate().isBefore(startDate) || semester.getEndDate().isAfter(endDate)) {
                throw new AcademicYearConflictException(
                        "Academic year dates must contain all of its semester dates."
                );
            }
        }
    }
}
