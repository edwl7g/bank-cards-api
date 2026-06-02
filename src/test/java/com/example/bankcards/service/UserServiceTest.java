package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.Account;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.repository.AccountRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User testUser;
    private Account testAccount;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser.setRole(UserRole.USER);

        testAccount = new Account();
        testAccount.setId(10L);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setUser(testUser);

        testCard = new Card();
        testCard.setId(100L);
        testCard.setNumCard("4123456789012345");
        testCard.setCardStatus(CardStatus.ACTIVE);
        testCard.setUser(testUser);
        testCard.setAccount(testAccount);
    }

    @Test
    void getUserCards_shouldReturnPageOfCards_whenUserExistsAndNoSearch() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUserId(userId, pageable)).thenReturn(cardPage);

        Page<CardResponseDto> result = userService.getUserCards(userId, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
        assertThat(result.getContent().get(0).maskedNumber()).isEqualTo(CardUtil.maskCardNumber(testCard.getNumCard()));
    }

    @Test
    void getUserCards_shouldReturnEmptyPage_whenSearchLengthNot4() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Page<CardResponseDto> result = userService.getUserCards(userId, "123", pageable);

        assertThat(result).isEmpty();
        verify(cardRepository, never()).findByUserIdAndNumCardLastFourHash(anyLong(), anyString(), any());
    }

    @Test
    void requestCardBlock_shouldBlockCard_whenCardBelongsToUserAndActive() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));

        userService.requestCardBlock(100L, 1L);

        assertThat(testCard.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).save(testCard);
    }

    @Test
    void requestCardBlock_shouldThrowException_whenCardDoesNotBelongToUser() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> userService.requestCardBlock(100L, 2L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Card does not belong to the user");
    }

    @Test
    void transferBetweenOwnCards_shouldTransferAmount_whenValid() {
        Long fromCardId = 100L;
        Long toCardId = 200L;
        BigDecimal amount = new BigDecimal("200.00");
        Long userId = 1L;

        Card toCard = new Card();
        toCard.setId(200L);
        toCard.setCardStatus(CardStatus.ACTIVE);
        toCard.setUser(testUser);
        Account toAccount = new Account();
        toAccount.setId(20L);
        toAccount.setBalance(new BigDecimal("500.00"));
        toCard.setAccount(toAccount);

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        userService.transferBetweenOwnCards(fromCardId, toCardId, amount, userId);

        assertThat(testAccount.getBalance()).isEqualTo(new BigDecimal("800.00"));
        assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal("700.00"));
        verify(accountRepository).save(testAccount);
        verify(accountRepository).save(toAccount);
    }

    @Test
    void transferBetweenOwnCards_shouldThrow_whenFromCardNotActive() {
        testCard.setCardStatus(CardStatus.BLOCKED);

        // Создаём и мокаем вторую карту (toCard), чтобы она существовала
        Card toCard = new Card();
        toCard.setId(200L);
        toCard.setCardStatus(CardStatus.ACTIVE);
        toCard.setUser(testUser); // устанавливаем того же пользователя
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(200L)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> userService.transferBetweenOwnCards(100L, 200L, BigDecimal.TEN, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("From card is not active");
    }

    @Test
    void getCardBalance_shouldReturnBalance_whenCardBelongsToUser() {
        when(cardRepository.findById(100L)).thenReturn(Optional.of(testCard));

        BigDecimal balance = userService.getCardBalance(100L, 1L);
        assertThat(balance).isEqualTo(new BigDecimal("1000.00"));
    }
}