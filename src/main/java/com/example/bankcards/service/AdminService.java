package com.example.bankcards.service;

import com.example.bankcards.dto.CreateAccountDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.dto.UserCreateAccountDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;

    private final AccountRepository accountRepository;

    private final CardRepository cardRepository;

    public AdminService(UserRepository userRepository, AccountRepository accountRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }



    /**
     * TODO:
     * ·	Администратор:
     * ·	  - Создает, блокирует, активирует, удаляет карты
     * ·	  - Управляет пользователями
     * ·	  - Видит все карты
     */
    @Transactional
    public Card createCard(
            CreateCardDto dto
    ) {
        //TODO 1. Проверяем существование пользователя
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + dto.userId()
                ));
        //TODO 2. Проверяем существование счета и принадлежность пользователю
        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + dto.accountId()));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Account does not belong to the specified user");
        }
        //TODO 3. Создаем новую карту
        Card card = new Card();
        card.setNumCard(generateCardNumber());
        card.setValidityPeriod(dto.validityPeriod());
        card.setCardStatus(CardStatus.ACTIVE);
        card.setUser(user);
        card.setAccount(account);
        //TODO 4. Сохраняем
        return cardRepository.save(card);
    }

    public void blockCard() {
    }

    public void activateCard() {
    }

    public void deleteCard() {
    }

    public List<Card> getAllCards() {
        return null;
    }

    @Transactional
    public CreateAccountDto createAccount(
            CreateAccountDto dto
    ) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + dto.userId()
                ));
        Account account = new Account();
        BigDecimal balance = (dto.initialBalance() != null)
                ? dto.initialBalance()
                : BigDecimal.ZERO;
        account.setBalance(balance);
        account.setUser(user);
        Account response = accountRepository.save(account);
        return new CreateAccountDto(
                response.getId(),
                response.getBalance()
        );
    }

    public UserResponseDto createUser(
            UserCreateAccountDto dto
    ) {
        User entity = userRepository.save(new User(dto));
        return new UserResponseDto(
                entity.getFirstName(),
                entity.getLastName(),
                entity.getRole(),
                entity.getAccount(),
                entity.getCard()
        );
    }
    public List<UserResponseDto> getAllUsers() {
        //TODO реализовать механизм Pageable
        return userRepository.findAll()
                .stream()
                .map(e -> new UserResponseDto(
                                e.getFirstName(),
                                e.getLastName(),
                                e.getRole(),
                                e.getAccount(),
                                e.getCard()
                        )
                ).collect(Collectors.toList());
    }

    public User updateUser() {
        return null;
    }

    public User deleteUser() {
        return null;
    }

    private String generateCardNumber() {
        Random random = new Random();
        // Префикс (первые 6 цифр) – можно сделать константным или случайным
        String prefix = "412345";  // например, для тестовой Visa
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = prefix.length(); i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        // Контрольная сумма Луна (добавим чуть позже, пока пропустим)
        return sb.toString().replaceAll("(.{4})", "$1-").replaceFirst("-$", ""); // 4 цифры + дефис
    }

}
