package com.chomnes.system.dto;

import com.chomnes.system.model.Subject;

public record SubjectResponse(
        Integer subjectId,
        String subjectCode,
        String subjectName,
        String description,
        Integer prerequisiteSubjectId
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.getSubjectId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getDescription(),
                subject.getPrerequisiteSubjectId()
        );
    }
}
