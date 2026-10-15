package com.chomnes.system.repository;

import com.chomnes.system.model.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonResourceRepository extends JpaRepository<LessonResource, Integer> {
    List<LessonResource> findAllByOrderByUploadDateDescResourceIdDesc();

    List<LessonResource> findAllByTeacherIdOrderByUploadDateDescResourceIdDesc(Integer teacherId);

    List<LessonResource> findAllByScheduleIdInOrderByUploadDateDescResourceIdDesc(List<Integer> scheduleIds);
}
