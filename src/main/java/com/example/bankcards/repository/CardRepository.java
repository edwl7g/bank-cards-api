package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findByUserId(Long userId, Pageable pageable);
    Page<Card> findByUserIdAndNumCardContainingIgnoreCase(Long userId, String numCard, Pageable pageable);
}
