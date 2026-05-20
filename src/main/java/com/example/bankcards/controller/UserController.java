package com.example.bankcards.controller;

import com.example.bankcards.dto.UserCreateAccountDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final AdminService adminService;
    private final UserService userService;

    public UserController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    // Регистрация нового пользователя (было)
    @PostMapping
    public UserResponseDto userCreateDto(@RequestBody UserCreateAccountDto dto) {
        return adminService.createUser(dto);
    }

    // Получение всех пользователей (только для админа? Оставим для MVP)
    @GetMapping
    public List<UserResponseDto> getUsers() {
        return adminService.getAllUsers();
    }

    // Просмотр своих карт (поиск + пагинация)
    @GetMapping("/cards")
    public Page<Card> getUserCards(@RequestParam Long userId,
                                   @RequestParam(required = false) String search,
                                   @PageableDefault(size = 10) Pageable pageable) {
        return userService.getUserCards(userId, search, pageable);
    }

    // Запрос блокировки карты
    @PostMapping("/cards/{cardId}/block-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestCardBlock(@PathVariable Long cardId, @RequestParam Long userId) {
        userService.requestCardBlock(cardId, userId);
    }

    // Перевод между своими картами
    @PostMapping("/cards/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferBetweenOwnCards(@RequestParam Long fromCardId,
                                        @RequestParam Long toCardId,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam Long userId) {
        userService.transferBetweenOwnCards(fromCardId, toCardId, amount, userId);
    }

    // Просмотр баланса карты
    @GetMapping("/cards/{cardId}/balance")
    public BigDecimal getCardBalance(@PathVariable Long cardId, @RequestParam Long userId) {
        return userService.getCardBalance(cardId, userId);
    }

    // Удаление карты (возможно, только админ? Оставим пока)
    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        adminService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
