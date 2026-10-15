package com.chomnes.system.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateLessonResourceRequest(
        @Positive Integer scheduleId,
        Boolean clearSchedule,
        @Size(max = 150) String title,
        String description,
        Boolean clearDescription,
        @Pattern(
                regexp = "LessonPlan|Homework|Syllabus|Other",
                message = "must be LessonPlan, Homework, Syllabus, or Other"
        ) String resourceType,
        @Size(max = 255) String fileUrl,
        Boolean clearFileUrl
) {
    public boolean isEmpty() {
        return scheduleId == null
                && clearSchedule == null
                && title == null
                && description == null
                && clearDescription == null
                && resourceType == null
                && fileUrl == null
                && clearFileUrl == null;
    }
}
