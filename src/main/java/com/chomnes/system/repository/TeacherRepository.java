package com.chomnes.system.repository;

import com.chomnes.system.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
    boolean existsByUserId(Integer userId);

    Optional<Teacher> findByUserId(Integer userId);
}
