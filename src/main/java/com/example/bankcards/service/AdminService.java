package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.util.CardUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class AdminService {

    private final UserRepository userRepository;

    private final AccountRepository accountRepository;

    private final CardRepository cardRepository;

    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, AccountRepository accountRepository, CardRepository cardRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Card createCard(
            CreateCardDto dto
    ) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + dto.userId()
                ));

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + dto.accountId()));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Account does not belong to the specified user");
        }

        Card card = new Card();
        card.setNumCard(generateCardNumber());
        card.setValidityPeriod(dto.validityPeriod());
        card.setCardStatus(CardStatus.ACTIVE);
        card.setUser(user);
        card.setAccount(account);

        return cardRepository.save(card);
    }

    @Transactional
    public void blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        card.setCardStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Transactional
    public void activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        if (card.getCardStatus() == CardStatus.ACTIVE) {
            throw new IllegalStateException("Card already active");
        }
        card.setCardStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        cardRepository.delete(card);
    }

    // Пагинированный список всех карт (для администратора)
    public Page<CardResponseDto> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(card -> new CardResponseDto(
                        card.getId(),
                        CardUtil.maskCardNumber(card.getNumCard()),
                        card.getValidityPeriod(),
                        card.getCardStatus(),
                        card.getUser().getId(),
                        card.getUser().getFirstName()
                                .concat(" ")
                                .concat(
                                        card.getUser().getLastName()),
                        card.getAccount().getId()
                ));
    }

    @Transactional
    public ResponseCreateBankAccountDto createAccount(
            CreateBankAccountDto dto
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
        return new ResponseCreateBankAccountDto(
                response.getId(),
                response.getBalance()
        );
    }

    @Transactional
    public UserResponseDto createUser(CreateUserDto dto) {
        // Запрещаем создание новых администраторов, если в БД уже есть хотя бы один ADMIN
        if (dto.userRole() == UserRole.ADMIN) {
            boolean adminExists = userRepository.existsByUserRole(UserRole.ADMIN);
            if (adminExists) {
                throw new IllegalStateException("Cannot create another admin. Only one admin is allowed.");
            }
        }
        User user = new User(dto);
        // пароль должен быть передан в dto, закодируем его
        user.setPassword(passwordEncoder.encode(dto.password()));
        // emailHash вычиститься автоматически в setEmail
        User saved = userRepository.save(user);
        return new UserResponseDto(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole(),
                saved.getAccount(),
                saved.getCard()
        );
    }

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> new UserResponseDto(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole(),
                        user.getAccount(),
                        user.getCard()
                ));
    }

    public Page<UserSummaryDto> getUsersByName(String firstname, String lastname, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending()); // или любая сортировка
        return userRepository.findByFirstNameAndLastName(firstname, lastname, pageable)
                .map(user -> new UserSummaryDto(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getIdentityDocumentNumber(),
                        user.getRole(),
                        user.getUserStatus()
                ));
    }

    // Обновление пользователя
    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        // Если пытаются установить роль ADMIN, но у пользователя она уже была другой
        if (dto.userRole() == UserRole.ADMIN && user.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Cannot promote user to ADMIN. Only one admin is allowed.");
        }

        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());
        user.setIdentityDocumentNumber(dto.identityDocumentNumber());
        user.setRole(dto.userRole());
        user.setUserStatus(dto.userStatus());

        User updated = userRepository.save(user);
        return new UserResponseDto(
                updated.getId(),
                updated.getFirstName(),
                updated.getLastName(),
                updated.getRole(),
                updated.getAccount(),
                updated.getCard()
        );
    }

    // Удаление пользователя
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("Cannot delete admin user");
        }

        // Получаем текущего аутентифицированного пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        if (currentUser.getUserId().equals(userId)) {
            throw new IllegalStateException("You cannot delete your own admin account");
        }

        // Проверка: есть ли счета с ненулевым балансом
        boolean hasNonZeroBalance = user.getAccount().stream()
                .anyMatch(acc -> acc.getBalance().compareTo(BigDecimal.ZERO) != 0);
        if (hasNonZeroBalance) {
            throw new IllegalStateException("Cannot delete user with non-zero balance accounts");
        }

        // Удаляем связанные карты и счета (если каскады не настроены)
        List<Card> cards = user.getCard();
        if (cards != null && !cards.isEmpty()) {
            cardRepository.deleteAll(cards);
        }
        List<Account> accounts = user.getAccount();
        if (accounts != null && !accounts.isEmpty()) {
            accountRepository.deleteAll(accounts);
        }
        userRepository.delete(user);
    }

    public CardDetailsDto getCardByIdForAdmin(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        return toCardDetailsDto(card, true);
    }

    public CardDetailsDto getCardByNumberForAdmin(String cardNumber) {
        String normalized = CardUtil.extractDigits(cardNumber);
        Card card = cardRepository.findByNumCardContaining(normalized)
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Card not found with number: " + cardNumber));
        return toCardDetailsDto(card, true);
    }

    private CardDetailsDto toCardDetailsDto(Card card, boolean showFullNumber) {
        String fullNumber = showFullNumber ? card.getNumCard() : null;
        return new CardDetailsDto(
                card.getId(),
                CardUtil.maskCardNumber(card.getNumCard()),
                fullNumber,
                card.getValidityPeriod(),
                card.getCardStatus(),
                card.getUser().getId(),
                card.getUser().getFirstName() + " " + card.getUser().getLastName(),
                card.getUser().getEmail(),
                card.getAccount().getId(),
                card.getAccount().getBalance()
        );
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
