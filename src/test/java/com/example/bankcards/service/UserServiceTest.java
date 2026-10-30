package com.example.bankcards.service;

import com.example.bankcards.dto.AccountResponseDto;
import com.example.bankcards.dto.CardDetailsDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.HmacUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты для UserService.
 * Требуется Mockito 5+ (по умолчанию в Spring Boot 3.x) для мокирования final-классов и records.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String CARD_NUMBER = "4123-4512-3456-7890";

    @Mock
    private CardRepository cardRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ---------------------------------------------------------------- helpers

    private User userWithId(Long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getFirstName()).thenReturn("Ivan");
        lenient().when(user.getLastName()).thenReturn("Petrov");
        lenient().when(user.getEmail()).thenReturn("ivan@mail.com");
        return user;
    }

    private Account accountWithBalance(String balance) {
        Account account = new Account();
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private Card realCard(User user, Account account, CardStatus status) {
        Card card = new Card();
        card.setUser(user);
        card.setAccount(account);
        card.setCardStatus(status);
        // numCard намеренно не задаём: сеттер может считать HMAC, а в этих тестах номер не нужен
        return card;
    }

    /** Мок карты, пригодный для маппинга в DTO (нужны id, номер и связанные сущности). */
    private Card mappableCard(User user) {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(5L);
        lenient().when(account.getBalance()).thenReturn(new BigDecimal("100"));

        Card card = mock(Card.class);
        lenient().when(card.getId()).thenReturn(10L);
        lenient().when(card.getNumCard()).thenReturn(CARD_NUMBER);
        lenient().when(card.getCardStatus()).thenReturn(CardStatus.ACTIVE);
        lenient().when(card.getUser()).thenReturn(user);
        lenient().when(card.getAccount()).thenReturn(account);
        return card;
    }

    // ---------------------------------------------------------------- getUserAccounts

    @Test
    @DisplayName("getUserAccounts: возвращает страницу счетов пользователя")
    void getUserAccounts_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(7L);
        when(account.getBalance()).thenReturn(new BigDecimal("50"));
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountRepository.findByUserId(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(account), pageable, 1));

        Page<AccountResponseDto> result = userService.getUserAccounts(USER_ID, pageable);

        assertEquals(1, result.getTotalElements());
        assertNotNull(result.getContent().get(0));
        verify(accountRepository).findByUserId(USER_ID, pageable);
    }

    @Test
    @DisplayName("getUserAccounts: пользователь не найден")
    void getUserAccounts_userNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> userService.getUserAccounts(USER_ID, PageRequest.of(0, 10)));
        verifyNoInteractions(accountRepository);
    }

    // ---------------------------------------------------------------- getUserCards

    @Test
    @DisplayName("getUserCards: пользователь не найден")
    void getUserCards_userNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getUserCards(USER_ID, null, PageRequest.of(0, 10)));
        verifyNoInteractions(cardRepository);
    }

    @Test
    @DisplayName("getUserCards: без поиска (null) возвращает все карты пользователя")
    void getUserCards_noSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = userWithId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Card card = mappableCard(user);
        when(cardRepository.findByUserId(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));

        Page<CardResponseDto> result = userService.getUserCards(USER_ID, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(cardRepository).findByUserId(USER_ID, pageable);
        verify(cardRepository, never()).findByUserIdAndNumCardLastFourHash(any(Long.class), any(String.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getUserCards: пустая/пробельная строка поиска трактуется как отсутствие поиска")
    void getUserCards_blankSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = userWithId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.findByUserId(USER_ID, pageable)).thenReturn(Page.empty(pageable));

        Page<CardResponseDto> result = userService.getUserCards(USER_ID, "   ", pageable);

        assertTrue(result.isEmpty());
        verify(cardRepository).findByUserId(USER_ID, pageable);
    }

    @Test
    @DisplayName("getUserCards: поиск не по 4 цифрам возвращает пустую страницу")
    void getUserCards_searchInvalidLength() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = userWithId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Page<CardResponseDto> result = userService.getUserCards(USER_ID, "12-345", pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(cardRepository);
    }

    @Test
    @DisplayName("getUserCards: поиск по последним 4 цифрам использует HMAC-хэш")
    void getUserCards_searchByLastFour() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = userWithId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Card card = mappableCard(user);
        when(cardRepository.findByUserIdAndNumCardLastFourHash(USER_ID, "hash", pageable))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));

        try (MockedStatic<HmacUtil> hmac = mockStatic(HmacUtil.class)) {
            hmac.when(() -> HmacUtil.hmac("7890")).thenReturn("hash");

            Page<CardResponseDto> result = userService.getUserCards(USER_ID, "78-90", pageable);

            assertEquals(1, result.getTotalElements());
            hmac.verify(() -> HmacUtil.hmac("7890"));
        }
        verify(cardRepository, never()).findByUserId(any(Long.class), any(Pageable.class));
    }

    // ---------------------------------------------------------------- requestCardBlock

    @Test
    @DisplayName("requestCardBlock: блокирует карту владельца")
    void requestCardBlock_success() {
        Card card = realCard(userWithId(USER_ID), null, CardStatus.ACTIVE);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        userService.requestCardBlock(10L, USER_ID);

        assertEquals(CardStatus.BLOCKED, card.getCardStatus());
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("requestCardBlock: карта не найдена")
    void requestCardBlock_notFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.requestCardBlock(10L, USER_ID));
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestCardBlock: карта принадлежит другому пользователю")
    void requestCardBlock_foreignCard() {
        Card card = realCard(userWithId(99L), null, CardStatus.ACTIVE);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        assertThrows(SecurityException.class, () -> userService.requestCardBlock(10L, USER_ID));
        assertEquals(CardStatus.ACTIVE, card.getCardStatus());
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestCardBlock: карта уже заблокирована")
    void requestCardBlock_alreadyBlocked() {
        Card card = realCard(userWithId(USER_ID), null, CardStatus.BLOCKED);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> userService.requestCardBlock(10L, USER_ID));
        verify(cardRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- transferBetweenOwnCards

    @Test
    @DisplayName("transfer: успешно переводит деньги между счетами")
    void transfer_success() {
        User user = userWithId(USER_ID);
        Account fromAccount = accountWithBalance("100.00");
        Account toAccount = accountWithBalance("20.00");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, fromAccount, CardStatus.ACTIVE)));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(realCard(user, toAccount, CardStatus.ACTIVE)));

        userService.transferBetweenOwnCards(1L, 2L, new BigDecimal("30.00"), USER_ID);

        assertEquals(0, new BigDecimal("70.00").compareTo(fromAccount.getBalance()));
        assertEquals(0, new BigDecimal("50.00").compareTo(toAccount.getBalance()));
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
    }

    @Test
    @DisplayName("transfer: можно перевести весь баланс")
    void transfer_entireBalance() {
        User user = userWithId(USER_ID);
        Account fromAccount = accountWithBalance("100.00");
        Account toAccount = accountWithBalance("0.00");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, fromAccount, CardStatus.ACTIVE)));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(realCard(user, toAccount, CardStatus.ACTIVE)));

        userService.transferBetweenOwnCards(1L, 2L, new BigDecimal("100.00"), USER_ID);

        assertEquals(0, BigDecimal.ZERO.compareTo(fromAccount.getBalance()));
        assertEquals(0, new BigDecimal("100.00").compareTo(toAccount.getBalance()));
    }

    @Test
    @DisplayName("transfer: сумма должна быть положительной (ноль)")
    void transfer_zeroAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.ZERO, USER_ID));
        verifyNoInteractions(cardRepository, accountRepository);
    }

    @Test
    @DisplayName("transfer: сумма должна быть положительной (отрицательная)")
    void transfer_negativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, new BigDecimal("-5"), USER_ID));
        verifyNoInteractions(cardRepository, accountRepository);
    }

    @Test
    @DisplayName("transfer: карта-источник не найдена")
    void transfer_fromCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
    }

    @Test
    @DisplayName("transfer: карта-получатель не найдена")
    void transfer_toCardNotFound() {
        User user = userWithId(USER_ID);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, accountWithBalance("100"), CardStatus.ACTIVE)));
        when(cardRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
    }

    @Test
    @DisplayName("transfer: карта-источник чужая")
    void transfer_fromCardForeign() {
        User foreignUser = userWithId(99L);
        User owner = userWithId(USER_ID);
        Card fromCard = realCard(foreignUser, accountWithBalance("100"), CardStatus.ACTIVE);
        Card toCard = realCard(owner, accountWithBalance("100"), CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(SecurityException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("transfer: карта-получатель чужая")
    void transfer_toCardForeign() {
        User owner = userWithId(USER_ID);
        User foreignUser = userWithId(99L);
        Card fromCard = realCard(owner, accountWithBalance("100"), CardStatus.ACTIVE);
        Card toCard = realCard(foreignUser, accountWithBalance("100"), CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(SecurityException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("transfer: карта-источник не активна")
    void transfer_fromCardNotActive() {
        User user = userWithId(USER_ID);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, accountWithBalance("100"), CardStatus.BLOCKED)));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(realCard(user, accountWithBalance("100"), CardStatus.ACTIVE)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
        assertEquals("From card is not active", ex.getMessage());
    }

    @Test
    @DisplayName("transfer: карта-получатель не активна")
    void transfer_toCardNotActive() {
        User user = userWithId(USER_ID);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, accountWithBalance("100"), CardStatus.ACTIVE)));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(realCard(user, accountWithBalance("100"), CardStatus.BLOCKED)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
        assertEquals("To card is not active", ex.getMessage());
    }

    @Test
    @DisplayName("transfer: карта не привязана к счёту")
    void transfer_cardWithoutAccount() {
        User user = userWithId(USER_ID);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, null, CardStatus.ACTIVE)));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(realCard(user, accountWithBalance("100"), CardStatus.ACTIVE)));

        assertThrows(IllegalStateException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, BigDecimal.TEN, USER_ID));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("transfer: недостаточно средств, балансы не меняются")
    void transfer_insufficientFunds() {
        User user = userWithId(USER_ID);
        Account fromAccount = accountWithBalance("10.00");
        Account toAccount = accountWithBalance("0.00");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(realCard(user, fromAccount, CardStatus.ACTIVE)));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(realCard(user, toAccount, CardStatus.ACTIVE)));

        assertThrows(IllegalArgumentException.class,
                () -> userService.transferBetweenOwnCards(1L, 2L, new BigDecimal("10.01"), USER_ID));

        assertEquals(0, new BigDecimal("10.00").compareTo(fromAccount.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(toAccount.getBalance()));
        verify(accountRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- getCardBalance

    @Test
    @DisplayName("getCardBalance: возвращает баланс связанного счёта")
    void getCardBalance_success() {
        Card card = realCard(userWithId(USER_ID), accountWithBalance("123.45"), CardStatus.ACTIVE);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        BigDecimal balance = userService.getCardBalance(10L, USER_ID);

        assertEquals(new BigDecimal("123.45"), balance);
    }

    @Test
    @DisplayName("getCardBalance: карта не найдена")
    void getCardBalance_notFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getCardBalance(10L, USER_ID));
    }

    @Test
    @DisplayName("getCardBalance: чужая карта")
    void getCardBalance_foreignCard() {
        Card card = realCard(userWithId(99L), accountWithBalance("1"), CardStatus.ACTIVE);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        assertThrows(SecurityException.class, () -> userService.getCardBalance(10L, USER_ID));
    }

    @Test
    @DisplayName("getCardBalance: карта не привязана к счёту")
    void getCardBalance_noAccount() {
        Card card = realCard(userWithId(USER_ID), null, CardStatus.ACTIVE);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> userService.getCardBalance(10L, USER_ID));
    }

    // ---------------------------------------------------------------- getCardByIdForUser

    @Test
    @DisplayName("getCardByIdForUser: возвращает детали карты владельца")
    void getCardByIdForUser_success() {
        User user = userWithId(USER_ID);
        Card card = mappableCard(user);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        CardDetailsDto result = userService.getCardByIdForUser(10L, USER_ID);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getCardByIdForUser: карта не найдена")
    void getCardByIdForUser_notFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getCardByIdForUser(10L, USER_ID));
    }

    @Test
    @DisplayName("getCardByIdForUser: чужая карта")
    void getCardByIdForUser_foreignCard() {
        Card card = mappableCard(userWithId(99L));
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        assertThrows(SecurityException.class, () -> userService.getCardByIdForUser(10L, USER_ID));
    }

    // ---------------------------------------------------------------- getCardByNumberForUser

    @Test
    @DisplayName("getCardByNumberForUser: находит карту по последним 4 цифрам")
    void getCardByNumberForUser_success() {
        User user = userWithId(USER_ID);
        Card card = mappableCard(user);
        when(cardRepository.findByUserIdAndNumCardLastFourHash(USER_ID, "hash"))
                .thenReturn(Optional.of(card));

        try (MockedStatic<HmacUtil> hmac = mockStatic(HmacUtil.class)) {
            hmac.when(() -> HmacUtil.hmac("7890")).thenReturn("hash");

            CardDetailsDto result = userService.getCardByNumberForUser("**** 7890", USER_ID);

            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("getCardByNumberForUser: передано не 4 цифры")
    void getCardByNumberForUser_invalidLength() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.getCardByNumberForUser("4123-4512-3456-7890", USER_ID));
        verifyNoInteractions(cardRepository);
    }

    @Test
    @DisplayName("getCardByNumberForUser: карта не найдена")
    void getCardByNumberForUser_notFound() {
        when(cardRepository.findByUserIdAndNumCardLastFourHash(USER_ID, "hash"))
                .thenReturn(Optional.empty());

        try (MockedStatic<HmacUtil> hmac = mockStatic(HmacUtil.class)) {
            hmac.when(() -> HmacUtil.hmac("7890")).thenReturn("hash");

            assertThrows(EntityNotFoundException.class,
                    () -> userService.getCardByNumberForUser("7890", USER_ID));
        }
    }
}
