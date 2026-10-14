package com.chomnes.system.service;

import com.chomnes.system.dto.CreateGradingCriterionRequest;
import com.chomnes.system.dto.GradingCriterionResponse;
import com.chomnes.system.dto.UpdateGradingCriterionRequest;
import com.chomnes.system.exception.GradingCriterionConflictException;
import com.chomnes.system.exception.GradingCriterionNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.SubjectNotFoundException;
import com.chomnes.system.model.GradingCriterion;
import com.chomnes.system.repository.GradingCriterionRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SubjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class GradingCriterionService {
    private static final BigDecimal MAX_TOTAL_WEIGHT = new BigDecimal("100.00");

    private final GradingCriterionRepository criterionRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;

    public GradingCriterionService(
            GradingCriterionRepository criterionRepository,
            SubjectRepository subjectRepository,
            SchoolClassRepository schoolClassRepository
    ) {
        this.criterionRepository = criterionRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional(readOnly = true)
    public List<GradingCriterionResponse> findAll() {
        return criterionRepository.findAll(Sort.by(Sort.Direction.ASC, "criteriaId"))
                .stream()
                .map(GradingCriterionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GradingCriterionResponse findById(Integer criteriaId) {
        return GradingCriterionResponse.from(getCriterion(criteriaId));
    }

    public GradingCriterionResponse create(CreateGradingCriterionRequest request) {
        String componentName = requireNonBlank(request.componentName());
        validateReferences(request.subjectId(), request.classId());
        ensureWeightTotal(request.subjectId(), request.classId(), request.weightPercentage(), null);

        GradingCriterion criterion = new GradingCriterion(
                request.subjectId(),
                request.classId(),
                componentName,
                request.weightPercentage(),
                request.attemptCount()
        );
        return GradingCriterionResponse.from(criterionRepository.save(criterion));
    }

    public GradingCriterionResponse update(Integer criteriaId, UpdateGradingCriterionRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one grading criterion field to update.");
        }
        if (Boolean.TRUE.equals(request.clearSubject()) && request.subjectId() != null) {
            throw new IllegalArgumentException("Set either subjectId or clearSubject, not both.");
        }
        if (Boolean.TRUE.equals(request.clearClass()) && request.classId() != null) {
            throw new IllegalArgumentException("Set either classId or clearClass, not both.");
        }

        GradingCriterion criterion = getCriterion(criteriaId);
        Integer subjectId = Boolean.TRUE.equals(request.clearSubject())
                ? null
                : request.subjectId() == null ? criterion.getSubjectId() : request.subjectId();
        Integer classId = Boolean.TRUE.equals(request.clearClass())
                ? null
                : request.classId() == null ? criterion.getClassId() : request.classId();
        String componentName = request.componentName() == null
                ? criterion.getComponentName()
                : requireNonBlank(request.componentName());
        BigDecimal weightPercentage = request.weightPercentage() == null
                ? criterion.getWeightPercentage()
                : request.weightPercentage();

        validateReferences(subjectId, classId);
        ensureWeightTotal(subjectId, classId, weightPercentage, criteriaId);

        criterion.setSubjectId(subjectId);
        criterion.setClassId(classId);
        criterion.setComponentName(componentName);
        criterion.setWeightPercentage(weightPercentage);
        if (request.attemptCount() != null) {
            criterion.setAttemptCount(request.attemptCount());
        }
        return GradingCriterionResponse.from(criterionRepository.save(criterion));
    }

    private GradingCriterion getCriterion(Integer criteriaId) {
        return criterionRepository.findById(criteriaId)
                .orElseThrow(() -> new GradingCriterionNotFoundException(criteriaId));
    }

    private void validateReferences(Integer subjectId, Integer classId) {
        if (subjectId != null && !subjectRepository.existsById(subjectId)) {
            throw new SubjectNotFoundException(subjectId);
        }
        if (classId != null && !schoolClassRepository.existsById(classId)) {
            throw new SchoolClassNotFoundException(classId);
        }
    }

    private void ensureWeightTotal(
            Integer subjectId,
            Integer classId,
            BigDecimal newWeight,
            Integer excludedCriteriaId
    ) {
        BigDecimal currentTotal = criterionRepository.findAll().stream()
                .filter(criterion -> Objects.equals(criterion.getSubjectId(), subjectId))
                .filter(criterion -> Objects.equals(criterion.getClassId(), classId))
                .filter(criterion -> !Objects.equals(criterion.getCriteriaId(), excludedCriteriaId))
                .map(GradingCriterion::getWeightPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentTotal.add(newWeight).compareTo(MAX_TOTAL_WEIGHT) > 0) {
            throw new GradingCriterionConflictException(
                    "The total grading weight for this subject and class cannot exceed 100 percent."
            );
        }
    }

    private String requireNonBlank(String componentName) {
        String trimmed = componentName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Component name must not be blank.");
        }
        return trimmed;
    }
}
