package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.util.HmacUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тесты для AdminService.
 * DTO-записи и сущности мокируются, чтобы тесты не зависели от конструкторов/сеттеров.
 * Требуется Mockito 5+ (по умолчанию в Spring Boot 3.x) — он умеет мокировать final-классы и records.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- createCard

    @Test
    @DisplayName("createCard: успешно создаёт активную карту с корректным номером")
    void createCard_success() {
        CreateCardDto dto = mock(CreateCardDto.class);
        when(dto.userId()).thenReturn(1L);
        when(dto.accountId()).thenReturn(2L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Account account = mock(Account.class);
        when(account.getUser()).thenReturn(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result;
        // setNumCard в сущности может считать HMAC; мок статики защищает от зависимости от конфигурации
        try (MockedStatic<HmacUtil> ignored = mockStatic(HmacUtil.class)) {
            result = adminService.createCard(dto);
        }

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        Card saved = captor.getValue();

        assertSame(result, saved);
        assertEquals(CardStatus.ACTIVE, saved.getCardStatus());
        assertSame(user, saved.getUser());
        assertSame(account, saved.getAccount());
        // формат: XXXX-XXXX-XXXX-XXXX, префикс 412345
        assertTrue(saved.getNumCard().matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}"),
                "Неверный формат номера: " + saved.getNumCard());
        assertTrue(saved.getNumCard().startsWith("4123-45"));
    }

    @Test
    @DisplayName("createCard: пользователь не найден")
    void createCard_userNotFound() {
        CreateCardDto dto = mock(CreateCardDto.class);
        when(dto.userId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.createCard(dto));
        verifyNoInteractions(accountRepository, cardRepository);
    }

    @Test
    @DisplayName("createCard: счёт не найден")
    void createCard_accountNotFound() {
        CreateCardDto dto = mock(CreateCardDto.class);
        when(dto.userId()).thenReturn(1L);
        when(dto.accountId()).thenReturn(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.createCard(dto));
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCard: счёт принадлежит другому пользователю")
    void createCard_accountBelongsToAnotherUser() {
        CreateCardDto dto = mock(CreateCardDto.class);
        when(dto.userId()).thenReturn(1L);
        when(dto.accountId()).thenReturn(2L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Account account = mock(Account.class);
        when(account.getUser()).thenReturn(owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> adminService.createCard(dto));
        verify(cardRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- block / activate / delete card

    @Test
    @DisplayName("blockCard: устанавливает статус BLOCKED")
    void blockCard_success() {
        Card card = new Card();
        card.setCardStatus(CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        adminService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, card.getCardStatus());
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("blockCard: карта не найдена")
    void blockCard_notFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.blockCard(1L));
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("activateCard: активирует заблокированную карту")
    void activateCard_success() {
        Card card = new Card();
        card.setCardStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        adminService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, card.getCardStatus());
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("activateCard: карта уже активна")
    void activateCard_alreadyActive() {
        Card card = new Card();
        card.setCardStatus(CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> adminService.activateCard(1L));
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("activateCard: карта не найдена")
    void activateCard_notFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.activateCard(1L));
    }

    @Test
    @DisplayName("deleteCard: удаляет карту")
    void deleteCard_success() {
        Card card = new Card();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        adminService.deleteCard(1L);

        verify(cardRepository).delete(card);
    }

    @Test
    @DisplayName("deleteCard: карта не найдена")
    void deleteCard_notFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.deleteCard(1L));
        verify(cardRepository, never()).delete(any(Card.class));
    }

    // ---------------------------------------------------------------- getAllCards

    @Test
    @DisplayName("getAllCards: маппит карты в DTO")
    void getAllCards_success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getFirstName()).thenReturn("Ivan");
        when(user.getLastName()).thenReturn("Petrov");
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(5L);

        Card card = mock(Card.class);
        when(card.getId()).thenReturn(10L);
        when(card.getNumCard()).thenReturn("4123-4512-3456-7890");
        when(card.getCardStatus()).thenReturn(CardStatus.ACTIVE);
        when(card.getUser()).thenReturn(user);
        when(card.getAccount()).thenReturn(account);

        Pageable pageable = PageRequest.of(0, 10);
        when(cardRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(card), pageable, 1));

        Page<CardResponseDto> result = adminService.getAllCards(pageable);

        assertEquals(1, result.getTotalElements());
        assertNotNull(result.getContent().get(0));
        // номер карты в ответе не должен совпадать с полным номером
        verify(cardRepository).findAll(pageable);
    }

    // ---------------------------------------------------------------- createAccount

    @Test
    @DisplayName("createAccount: использует переданный начальный баланс")
    void createAccount_withInitialBalance() {
        CreateBankAccountDto dto = mock(CreateBankAccountDto.class);
        when(dto.userId()).thenReturn(1L);
        when(dto.initialBalance()).thenReturn(new BigDecimal("150.50"));
        User user = mock(User.class);
        Account savedAccount = mock(Account.class);
        when(savedAccount.getId()).thenReturn(7L);
        when(savedAccount.getBalance()).thenReturn(new BigDecimal("150.50"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        ResponseCreateBankAccountDto result = adminService.createAccount(dto);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(new BigDecimal("150.50"), captor.getValue().getBalance());
        assertSame(user, captor.getValue().getUser());
        assertNotNull(result);
    }

    @Test
    @DisplayName("createAccount: при null балансе ставит 0")
    void createAccount_nullBalance() {
        CreateBankAccountDto dto = mock(CreateBankAccountDto.class);
        when(dto.userId()).thenReturn(1L);
        when(dto.initialBalance()).thenReturn(null);
        Account savedAccount = mock(Account.class);
        when(savedAccount.getId()).thenReturn(7L);
        when(savedAccount.getBalance()).thenReturn(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        adminService.createAccount(dto);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getBalance()));
    }

    @Test
    @DisplayName("createAccount: пользователь не найден")
    void createAccount_userNotFound() {
        CreateBankAccountDto dto = mock(CreateBankAccountDto.class);
        when(dto.userId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.createAccount(dto));
        verify(accountRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- createUser

    @Test
    @DisplayName("createUser: нельзя создать второго администратора")
    void createUser_secondAdminForbidden() {
        CreateUserDto dto = mock(CreateUserDto.class);
        when(dto.userRole()).thenReturn(UserRole.ADMIN);
        when(userRepository.existsByUserRole(UserRole.ADMIN)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> adminService.createUser(dto));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // Успешный сценарий createUser не покрыт: он вызывает new User(dto) и сеттер email
    // (вычисление HMAC), поэтому зависит от реализации сущности User и конфигурации HmacUtil.

    // ---------------------------------------------------------------- getAllUsers / getUsersByName

    @Test
    @DisplayName("getAllUsers: возвращает страницу DTO")
    void getAllUsers_success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Pageable pageable = PageRequest.of(0, 5);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<UserResponseDto> result = adminService.getAllUsers(pageable);

        assertEquals(1, result.getContent().size());
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getUsersByName: ищет по имени и фамилии с пагинацией")
    void getUsersByName_success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findByFirstNameAndLastName(eq("Ivan"), eq("Petrov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserSummaryDto> result = adminService.getUsersByName("Ivan", "Petrov", 0, 10);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByFirstNameAndLastName(eq("Ivan"), eq("Petrov"), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    // ---------------------------------------------------------------- updateUser

    @Test
    @DisplayName("updateUser: обновляет поля обычного пользователя")
    void updateUser_success() {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserUpdateDto dto = mock(UserUpdateDto.class);
        when(dto.userRole()).thenReturn(UserRole.USER);
        when(dto.firstName()).thenReturn("New");
        when(dto.lastName()).thenReturn("Name");
        when(dto.email()).thenReturn("new@mail.com");
        when(dto.phone()).thenReturn("+123");
        when(dto.identityDocumentNumber()).thenReturn("DOC1");
        when(dto.userStatus()).thenReturn(UserStatus.ACTIVE);

        UserResponseDto result = adminService.updateUser(1L, dto);

        assertNotNull(result);
        verify(user).setFirstName("New");
        verify(user).setLastName("Name");
        verify(user).setEmail("new@mail.com");
        verify(user).setPhone("+123");
        verify(user).setIdentityDocumentNumber("DOC1");
        verify(user).setRole(UserRole.USER);
        verify(user).setUserStatus(UserStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateUser: пользователь не найден")
    void updateUser_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminService.updateUser(1L, mock(UserUpdateDto.class)));
    }

    @Test
    @DisplayName("updateUser: нельзя менять роль администратора")
    void updateUser_cannotChangeAdminRole() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        UserUpdateDto dto = mock(UserUpdateDto.class);
        when(dto.userRole()).thenReturn(UserRole.USER);

        assertThrows(IllegalStateException.class, () -> adminService.updateUser(1L, dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: нельзя повысить пользователя до ADMIN")
    void updateUser_cannotPromoteToAdmin() {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserUpdateDto dto = mock(UserUpdateDto.class);
        when(dto.userRole()).thenReturn(UserRole.ADMIN);

        assertThrows(IllegalStateException.class, () -> adminService.updateUser(1L, dto));
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- deleteUser

    private void authenticateAs(Long currentUserId) {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getUserId()).thenReturn(currentUserId);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("deleteUser: удаляет карты, счета и пользователя")
    void deleteUser_success() {
        authenticateAs(100L);

        Account account = mock(Account.class);
        when(account.getBalance()).thenReturn(BigDecimal.ZERO);
        Card card = mock(Card.class);
        List<Account> accounts = List.of(account);
        List<Card> cards = List.of(card);

        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getAccount()).thenReturn(accounts);
        when(user.getCard()).thenReturn(cards);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.deleteUser(1L);

        verify(cardRepository).deleteAll(cards);
        verify(accountRepository).deleteAll(accounts);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser: пользователь не найден")
    void deleteUser_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.deleteUser(1L));
    }

    @Test
    @DisplayName("deleteUser: нельзя удалить администратора")
    void deleteUser_adminForbidden() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class, () -> adminService.deleteUser(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser: нельзя удалить самого себя")
    void deleteUser_selfDeleteForbidden() {
        authenticateAs(1L);
        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> adminService.deleteUser(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser: нельзя удалить пользователя со счетом с ненулевым балансом")
    void deleteUser_nonZeroBalance() {
        authenticateAs(100L);
        Account account = mock(Account.class);
        when(account.getBalance()).thenReturn(new BigDecimal("10.00"));
        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getAccount()).thenReturn(List.of(account));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> adminService.deleteUser(1L));
        verify(cardRepository, never()).deleteAll(any());
        verify(accountRepository, never()).deleteAll(any());
        verify(userRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- getCardBy...ForAdmin

    private Card buildFullCard() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getFirstName()).thenReturn("Ivan");
        when(user.getLastName()).thenReturn("Petrov");
        when(user.getEmail()).thenReturn("ivan@mail.com");
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(5L);
        when(account.getBalance()).thenReturn(new BigDecimal("100"));

        Card card = mock(Card.class);
        when(card.getId()).thenReturn(10L);
        when(card.getNumCard()).thenReturn("4123-4512-3456-7890");
        when(card.getCardStatus()).thenReturn(CardStatus.ACTIVE);
        when(card.getUser()).thenReturn(user);
        when(card.getAccount()).thenReturn(account);
        return card;
    }

    @Test
    @DisplayName("getCardByIdForAdmin: возвращает детали карты")
    void getCardByIdForAdmin_success() {
        Card card = buildFullCard();
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        CardDetailsDto result = adminService.getCardByIdForAdmin(10L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getCardByIdForAdmin: карта не найдена")
    void getCardByIdForAdmin_notFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.getCardByIdForAdmin(10L));
    }

    @Test
    @DisplayName("getCardByNumberForAdmin: находит карту по номеру")
    void getCardByNumberForAdmin_success() {
        Card card = buildFullCard();
        when(cardRepository.findByNumCardContaining(any())).thenReturn(List.of(card));

        CardDetailsDto result = adminService.getCardByNumberForAdmin("4123-4512-3456-7890");

        assertNotNull(result);
        verify(cardRepository).findByNumCardContaining(any());
    }

    @Test
    @DisplayName("getCardByNumberForAdmin: карта не найдена")
    void getCardByNumberForAdmin_notFound() {
        when(cardRepository.findByNumCardContaining(any())).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class,
                () -> adminService.getCardByNumberForAdmin("4123-4512-3456-7890"));
    }

    // ---------------------------------------------------------------- getUserForUpdate

    @Test
    @DisplayName("getUserForUpdate: возвращает DTO для редактирования")
    void getUserForUpdate_success() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateDto result = adminService.getUserForUpdate(1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getUserForUpdate: пользователь не найден")
    void getUserForUpdate_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.getUserForUpdate(1L));
    }

    // ---------------------------------------------------------------- registerUser

    @Test
    @DisplayName("registerUser: сохраняет нового пользователя с ролью USER и статусом ACTIVE")
    void registerUser_success() {
        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn("test@mail.com");
        when(dto.getFirstName()).thenReturn("Ivan");
        when(dto.getLastName()).thenReturn("Petrov");
        when(dto.getPassword()).thenReturn("secret");
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.findByEmailHash("hash")).thenReturn(Optional.empty());

        try (MockedStatic<HmacUtil> hmac = mockStatic(HmacUtil.class)) {
            hmac.when(() -> HmacUtil.hmac("test@mail.com")).thenReturn("hash");

            adminService.registerUser(dto);
        }

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Ivan", saved.getFirstName());
        assertEquals("Petrov", saved.getLastName());
        verify(passwordEncoder).encode("secret");
        assertEquals(UserRole.USER, saved.getRole());
        assertEquals(UserStatus.ACTIVE, saved.getUserStatus());
    }

    @Test
    @DisplayName("registerUser: email уже зарегистрирован")
    void registerUser_emailExists() {
        UserRegistrationDto dto = mock(UserRegistrationDto.class);
        when(dto.getEmail()).thenReturn("test@mail.com");
        when(userRepository.findByEmailHash("hash")).thenReturn(Optional.of(mock(User.class)));

        try (MockedStatic<HmacUtil> hmac = mockStatic(HmacUtil.class)) {
            hmac.when(() -> HmacUtil.hmac("test@mail.com")).thenReturn("hash");

            assertThrows(RuntimeException.class, () -> adminService.registerUser(dto));
        }
        verify(userRepository, never()).save(any());
    }
}
