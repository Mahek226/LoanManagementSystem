package com.tss.springsecurity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tss.springsecurity.entity.Applicant;

@Repository
public interface UserRepository extends JpaRepository<Applicant, Long> {
    Optional<Applicant> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}
