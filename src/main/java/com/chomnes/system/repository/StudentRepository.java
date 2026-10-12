package com.chomnes.system.repository;

import com.chomnes.system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    boolean existsByUserId(Integer userId);

    Optional<Student> findByUserId(Integer userId);
}
