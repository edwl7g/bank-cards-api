package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public UserService(CardRepository cardRepository,
                       AccountRepository accountRepository,
                       UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // Просмотр своих карт с поиском по номеру карты и пагинацией
    public Page<Card> getUserCards(Long userId, String search, Pageable pageable) {
        // Проверяем существование пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (search != null && !search.isBlank()) {
            // Поиск по номеру карты (игнорируя дефисы/пробелы)
            String normalizedSearch = search.replaceAll("[-\\s]", "");
            return cardRepository.findByUserIdAndNumCardContainingIgnoreCase(userId, normalizedSearch, pageable);
        } else {
            return cardRepository.findByUserId(userId, pageable);
        }
    }

    // Запрос блокировки карты (меняем статус на BLOCKED)
    @Transactional
    public void requestCardBlock(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        if (!card.getUser().getId().equals(userId)) {
            throw new SecurityException("Card does not belong to the user");
        }
        if (card.getCardStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Card already blocked");
        }
        card.setCardStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    // Перевод между своими картами
    @Transactional
    public void transferBetweenOwnCards(Long fromCardId, Long toCardId, BigDecimal amount, Long userId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Card fromCard = cardRepository.findById(fromCardId)
                .orElseThrow(() -> new EntityNotFoundException("From card not found: " + fromCardId));
        Card toCard = cardRepository.findById(toCardId)
                .orElseThrow(() -> new EntityNotFoundException("To card not found: " + toCardId));

        // Проверяем, что обе карты принадлежат одному пользователю
        if (!fromCard.getUser().getId().equals(userId) || !toCard.getUser().getId().equals(userId)) {
            throw new SecurityException("Both cards must belong to the user");
        }

        // Проверяем, что карты активны
        if (fromCard.getCardStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("From card is not active");
        }
        if (toCard.getCardStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("To card is not active");
        }

        // Получаем счета, связанные с картами
        Account fromAccount = fromCard.getAccount();
        Account toAccount = toCard.getAccount();
        if (fromAccount == null || toAccount == null) {
            throw new IllegalStateException("Card not linked to account");
        }

        // Проверка достаточности средств
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Выполняем перевод
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    // Получение баланса карты (через связанный счёт)
    public BigDecimal getCardBalance(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        if (!card.getUser().getId().equals(userId)) {
            throw new SecurityException("Card does not belong to the user");
        }
        Account account = card.getAccount();
        if (account == null) {
            throw new IllegalStateException("Card not linked to account");
        }
        return account.getBalance();
    }
}