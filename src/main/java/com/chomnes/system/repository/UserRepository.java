package com.chomnes.system.repository;

import com.chomnes.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndUserIdNot(String username, Integer userId);

    boolean existsByEmailAndUserIdNot(String email, Integer userId);
}
