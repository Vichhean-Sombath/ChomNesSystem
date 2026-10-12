package com.chomnes.system.repository;

import com.chomnes.system.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    boolean existsBySubjectCode(String subjectCode);

    boolean existsBySubjectCodeAndSubjectIdNot(String subjectCode, Integer subjectId);
}
