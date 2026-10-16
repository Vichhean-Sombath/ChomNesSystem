package com.chomnes.system.service;

import com.chomnes.system.dto.CreateSubjectRequest;
import com.chomnes.system.dto.SubjectResponse;
import com.chomnes.system.dto.UpdateSubjectRequest;
import com.chomnes.system.exception.SubjectConflictException;
import com.chomnes.system.exception.SubjectNotFoundException;
import com.chomnes.system.model.Subject;
import com.chomnes.system.repository.SubjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class SubjectService {
    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "subjectCode"))
                .stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse findById(Integer subjectId) {
        return SubjectResponse.from(getSubject(subjectId));
    }

    public SubjectResponse create(CreateSubjectRequest request) {
        String subjectCode = requireNonBlank(request.subjectCode(), "Subject code");
        String subjectName = requireNonBlank(request.subjectName(), "Subject name");
        if (subjectRepository.existsBySubjectCode(subjectCode)) {
            throw new SubjectConflictException("A subject with this code already exists.");
        }
        validatePrerequisiteExists(request.prerequisiteSubjectId());

        Subject subject = new Subject(
                subjectCode,
                subjectName,
                request.description(),
                request.prerequisiteSubjectId()
        );
        return SubjectResponse.from(subjectRepository.save(subject));
    }

    public SubjectResponse update(Integer subjectId, UpdateSubjectRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one subject field to update.");
        }
        if (Boolean.TRUE.equals(request.clearPrerequisiteSubject())
                && request.prerequisiteSubjectId() != null) {
            throw new IllegalArgumentException(
                    "Set either prerequisiteSubjectId or clearPrerequisiteSubject, not both."
            );
        }
        if (Boolean.TRUE.equals(request.clearDescription()) && request.description() != null) {
            throw new IllegalArgumentException("Set either description or clearDescription, not both.");
        }

        Subject subject = getSubject(subjectId);
        String subjectCode = request.subjectCode() == null
                ? subject.getSubjectCode()
                : requireNonBlank(request.subjectCode(), "Subject code");
        String subjectName = request.subjectName() == null
                ? subject.getSubjectName()
                : requireNonBlank(request.subjectName(), "Subject name");
        if (!subjectCode.equals(subject.getSubjectCode())
                && subjectRepository.existsBySubjectCodeAndSubjectIdNot(subjectCode, subjectId)) {
            throw new SubjectConflictException("A subject with this code already exists.");
        }

        Integer prerequisiteSubjectId = Boolean.TRUE.equals(request.clearPrerequisiteSubject())
                ? null
                : request.prerequisiteSubjectId() == null
                        ? subject.getPrerequisiteSubjectId()
                        : request.prerequisiteSubjectId();
        validatePrerequisite(subjectId, prerequisiteSubjectId);

        subject.setSubjectCode(subjectCode);
        subject.setSubjectName(subjectName);
        if (Boolean.TRUE.equals(request.clearDescription())) {
            subject.setDescription(null);
        } else if (request.description() != null) {
            subject.setDescription(request.description());
        }
        subject.setPrerequisiteSubjectId(prerequisiteSubjectId);
        return SubjectResponse.from(subjectRepository.save(subject));
    }

    public void delete(Integer subjectId) {
        getSubject(subjectId);
        subjectRepository.deleteById(subjectId);
    }

    private Subject getSubject(Integer subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    private void validatePrerequisiteExists(Integer prerequisiteSubjectId) {
        if (prerequisiteSubjectId != null && !subjectRepository.existsById(prerequisiteSubjectId)) {
            throw new SubjectNotFoundException(prerequisiteSubjectId);
        }
    }

    private void validatePrerequisite(Integer subjectId, Integer prerequisiteSubjectId) {
        if (prerequisiteSubjectId == null) {
            return;
        }
        if (subjectId.equals(prerequisiteSubjectId)) {
            throw new SubjectConflictException("A subject cannot be its own prerequisite.");
        }

        Set<Integer> visited = new HashSet<>();
        Integer currentId = prerequisiteSubjectId;
        while (currentId != null && visited.add(currentId)) {
            if (subjectId.equals(currentId)) {
                throw new SubjectConflictException("Subject prerequisites cannot form a cycle.");
            }
            currentId = getSubject(currentId).getPrerequisiteSubjectId();
        }
    }

    private String requireNonBlank(String value, String field) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return trimmed;
    }
}
