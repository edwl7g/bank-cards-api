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
import com.example.bankcards.util.CardUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CardRepository cardRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminService adminService;

    private User testUser;
    private Account testAccount;
    private Card testCard;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");
        testUser.setRole(UserRole.USER);
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser.setAccount(new ArrayList<>());  // инициализируем коллекции
        testUser.setCard(new ArrayList<>());

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setAccount(new ArrayList<>());
        adminUser.setCard(new ArrayList<>());

        testAccount = new Account();
        testAccount.setId(10L);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setUser(testUser);
        testUser.getAccount().add(testAccount);

        testCard = new Card();
        testCard.setId(100L);
        testCard.setNumCard("4123456789012345");
        testCard.setCardStatus(CardStatus.ACTIVE);
        testCard.setUser(testUser);
        testCard.setAccount(testAccount);
        testUser.getCard().add(testCard);
    }

    // ========== СОЗДАНИЕ КАРТЫ ==========
    @Test
    void createCard_shouldCreateAndReturnCard_whenValidDto() {
        CreateCardDto dto = new CreateCardDto(1L, 10L, LocalDateTime.now().plusYears(5));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(testAccount));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card created = adminService.createCard(dto);

        assertThat(created.getUser()).isEqualTo(testUser);
        assertThat(created.getAccount()).isEqualTo(testAccount);
        assertThat(created.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(created.getNumCard()).matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}");
        verify(cardRepository).save(any(Card.class));
    }

    // ========== БЛОКИРОВКА КАРТЫ ==========
    @Test
    void blockCard_shouldChangeStatusToBlocked() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));
        adminService.blockCard(100L);
        assertThat(testCard.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).save(testCard);
    }

    // ========== АКТИВАЦИЯ КАРТЫ ==========
    @Test
    void activateCard_shouldChangeStatusToActive_whenBlocked() {
        testCard.setCardStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));
        adminService.activateCard(100L);
        assertThat(testCard.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository).save(testCard);
    }

    // ========== УДАЛЕНИЕ КАРТЫ ==========
    @Test
    void deleteCard_shouldDeleteCard() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));
        adminService.deleteCard(100L);
        verify(cardRepository).delete(testCard);
    }

    // ========== ПРОСМОТР ВСЕХ КАРТ (ПАГИНАЦИЯ) ==========
    @Test
    void getAllCards_shouldReturnPageOfCardResponseDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));
        when(cardRepository.findAll(pageable)).thenReturn(cardPage);

        Page<CardResponseDto> result = adminService.getAllCards(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
    }

    // ========== СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @Test
    void createUser_shouldSaveUserWithEncodedPassword_whenValidDto() {
        CreateUserDto dto = new CreateUserDto("Jane", "Smith", "jane@example.com", "1234567890", "ID123",
                UserRole.USER, UserStatus.ACTIVE, "password");
        when(passwordEncoder.encode("password")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto result = adminService.createUser(dto);

        assertThat(result.firstName()).isEqualTo("Jane");
        assertThat(result.userRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrow_whenTryToCreateSecondAdmin() {
        when(userRepository.existsByUserRole(UserRole.ADMIN)).thenReturn(true);
        CreateUserDto dto = new CreateUserDto("Bad", "Admin", "admin2@example.com", "", "",
                UserRole.ADMIN, UserStatus.ACTIVE, "pass");

        assertThatThrownBy(() -> adminService.createUser(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create another admin. Only one admin is allowed.");
    }

    // ========== ОБНОВЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @Test
    void updateUser_shouldUpdateUserFields() {
        Long userId = 1L;
        UserUpdateDto dto = new UserUpdateDto("Updated", "User", "new@email.com", "12345", "doc123",
                UserRole.USER, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDto result = adminService.updateUser(userId, dto);

        assertThat(result.firstName()).isEqualTo("Updated");
        verify(userRepository).save(testUser);
    }

    // ========== УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @Test
    void deleteUser_shouldDeleteUser_whenNotAdminAndNoBalance() {
        // Устанавливаем баланс счёта в 0
        testAccount.setBalance(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Мокируем SecurityContextHolder, чтобы deleteUser получил текущего пользователя (не удаляющего самого себя)
        Authentication auth = mock(Authentication.class);
        CustomUserDetails currentUserDetails = new CustomUserDetails(adminUser); // adminUser с id=99
        when(auth.getPrincipal()).thenReturn(currentUserDetails);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        adminService.deleteUser(1L);

        verify(cardRepository).deleteAll(anyList());
        verify(accountRepository).deleteAll(anyList());
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_shouldThrow_whenUserHasNonZeroBalance() {
        // Баланс > 0
        testAccount.setBalance(new BigDecimal("100.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Мокируем SecurityContextHolder (чтобы не было NPE, но в этом тесте условие с балансом сработает раньше)
        Authentication auth = mock(Authentication.class);
        CustomUserDetails currentUserDetails = new CustomUserDetails(adminUser);
        when(auth.getPrincipal()).thenReturn(currentUserDetails);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete user with non-zero balance accounts");
    }

    @Test
    void deleteUser_shouldThrow_whenTryingToDeleteAdmin() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.deleteUser(99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete admin user");
    }
}