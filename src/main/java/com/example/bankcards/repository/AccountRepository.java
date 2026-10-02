package com.example.bankcards.repository;

import com.example.bankcards.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,Long> {
    Page<Account> findByUserId(Long userId, Pageable pageable);
}
