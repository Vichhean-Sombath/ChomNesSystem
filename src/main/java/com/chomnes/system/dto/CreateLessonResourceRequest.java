package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLessonResourceRequest(
    @Positive Integer teacherId,
    @Positive Integer scheduleId,
    @NotBlank @Size(max = 150) String title,
    String description,
    @NotBlank @Pattern(
        regexp = "LessonPlan|Homework|Syllabus|Other",
        message = "must be LessonPlan, Homework, Syllabus, or Other"
    ) String resourceType,
    @Size(max = 255) String fileUrl
) {
}
