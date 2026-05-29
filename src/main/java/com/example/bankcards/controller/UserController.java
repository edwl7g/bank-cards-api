package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/user")
@Validated
public class UserController {

    private final AdminService adminService;
    private final UserService userService;

    public UserController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    // Регистрация нового пользователя (было)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto userCreateDto(@RequestBody @Valid CreateUserDto dto) {
        return adminService.createUser(dto);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto updateUser(@PathVariable Long userId, @RequestBody @Valid UserUpdateDto dto) {
        return adminService.updateUser(userId, dto);
    }

    // Получение всех пользователей (только для админа? Оставим для MVP)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponseDto> getUsers(Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    @GetMapping("/find")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserSummaryDto> getUser(@RequestParam("firstname") String firstName,
                                        @RequestParam("lastname") String lastName,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return adminService.getUsersByName(firstName, lastName, page, size);
    }

    // Просмотр своих карт (поиск + пагинация)
    @GetMapping("/cards")
    @PreAuthorize("isAuthenticated()")
    public Page<CardResponseDto> getUserCards(@AuthenticationPrincipal CustomUserDetails currentUser,
                                              @RequestParam(required = false) String search,
                                              @PageableDefault(size = 10) Pageable pageable) {
        return userService.getUserCards(currentUser.getUserId(), search, pageable);
    }

    @PostMapping("/cards/{cardId}/block-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void requestCardBlock(@PathVariable Long cardId,
                                 @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.requestCardBlock(cardId, currentUser.getUserId());
    }

    // Перевод между своими картами
    @PostMapping("/cards/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void transferBetweenOwnCards(@RequestParam @NotNull Long fromCardId,
                                        @RequestParam @NotNull Long toCardId,
                                        @RequestParam @Positive BigDecimal amount,
                                        @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.transferBetweenOwnCards(fromCardId, toCardId, amount, currentUser.getUserId());
    }

    // Просмотр баланса карты
    @GetMapping("/cards/{cardId}/balance")
    @PreAuthorize("isAuthenticated()")
    public BigDecimal getCardBalance(@PathVariable Long cardId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return userService.getCardBalance(cardId, currentUser.getUserId());
    }

    // Удаление карты (возможно, только админ? Оставим пока)
    @DeleteMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        adminService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
    }

    // UserController.java
    @GetMapping("/cards/{cardId}")
    @PreAuthorize("isAuthenticated()")
    public CardDetailsDto getCardById(@PathVariable Long cardId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return userService.getCardByIdForUser(cardId, currentUser.getUserId());
    }

    @GetMapping("/cards/by-number")
    @PreAuthorize("isAuthenticated()")
    public CardDetailsDto getCardByNumber(@RequestParam("number") String cardNumber, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return userService.getCardByNumberForUser(cardNumber, currentUser.getUserId());
    }

}