package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public class UserService {
    Page<Card> getUserCards(Long userId, String search, Pageable pageable) {
        return null;
    }
    void requestCardBlock(Long cardId) {}
    void transferBetweenOwnCards(Long fromCardId, Long toCardId, BigDecimal amount) {}
    BigDecimal getCardBalance(Long cardId) {
        return null;
    }
    /**
     * TODO
     * ·	  - Просматривает свои карты (поиск + пагинация)
     * ·	  - Запрашивает блокировку карты
     * ·	  - Делает переводы между своими картами
     * ·	  - Смотрит баланс
     */
}
