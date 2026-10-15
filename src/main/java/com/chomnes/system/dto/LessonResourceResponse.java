package com.chomnes.system.dto;

import com.chomnes.system.model.LessonResource;

import java.time.LocalDateTime;

public record LessonResourceResponse(
        Integer resourceId,
        Integer scheduleId,
        Integer teacherId,
        String title,
        String description,
        String resourceType,
        String fileUrl,
        LocalDateTime uploadDate
) {
    public static LessonResourceResponse from(LessonResource resource) {
        return new LessonResourceResponse(
                resource.getResourceId(),
                resource.getScheduleId(),
                resource.getTeacherId(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getResourceType(),
                resource.getFileUrl(),
                resource.getUploadDate()
        );
    }
}
