package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findByUserId(Long userId, Pageable pageable);

    Page<Card> findByUserIdAndNumCardContainingIgnoreCase(Long userId, String numCard, Pageable pageable);

    List<Card> findByNumCardContaining(String numCard);

    // Для пагинации при поиске (список карт пользователя с точным совпадением хеша)
    Page<Card> findByUserIdAndNumCardLastFourHash(Long userId, String numCardLastFourHash, Pageable pageable);

    Optional<Card> findByUserIdAndNumCardLastFourHash(Long userId, String numCardLastFourHash);
}