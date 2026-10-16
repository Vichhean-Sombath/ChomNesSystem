package com.chomnes.system.repository;

import com.chomnes.system.model.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonResourceRepository extends JpaRepository<LessonResource, Integer> {
    Optional<LessonResource> findByFileUrl(String fileUrl);

    List<LessonResource> findAllByOrderByUploadDateDescResourceIdDesc();

    List<LessonResource> findAllByTeacherIdOrderByUploadDateDescResourceIdDesc(Integer teacherId);

    List<LessonResource> findAllByScheduleIdInOrderByUploadDateDescResourceIdDesc(List<Integer> scheduleIds);
}
