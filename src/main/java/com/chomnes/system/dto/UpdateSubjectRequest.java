package com.chomnes.system.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(
    @Size(min = 1, max = 20) String subjectCode,
    @Size(min = 1, max = 100) String subjectName,
    String description,
    @Positive Integer prerequisiteSubjectId,
    Boolean clearPrerequisiteSubject,
    Boolean clearDescription
) {
    public boolean isEmpty() {
        return subjectCode == null
            && subjectName == null
            && description == null
            && prerequisiteSubjectId == null
            && clearPrerequisiteSubject == null
            && clearDescription == null;
    }
}
