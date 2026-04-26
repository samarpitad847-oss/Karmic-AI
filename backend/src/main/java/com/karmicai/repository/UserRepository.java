package com.karmicai.repository;

import com.karmicai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailHash(String emailHash);
    boolean existsByEmailHash(String emailHash);
}
