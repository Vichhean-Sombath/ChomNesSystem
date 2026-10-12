package com.chomnes.system.repository;

import com.chomnes.system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    boolean existsByUserId(Integer userId);
}
