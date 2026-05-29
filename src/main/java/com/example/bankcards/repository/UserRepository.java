package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Page<User> findByFirstNameAndLastName(String firstname, String lastname, Pageable pageable);
    Optional<User> findByEmailHash(String emailHash);
    boolean existsByUserRole(UserRole role);
}