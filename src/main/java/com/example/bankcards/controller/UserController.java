package com.example.bankcards.controller;

import com.example.bankcards.dto.CreateUserDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.dto.UserUpdateDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
    public UserResponseDto userCreateDto(@RequestBody CreateUserDto dto) {
        return adminService.createUser(dto);
    }

    @PutMapping("/{userId}")
    public UserResponseDto updateUser(@PathVariable Long userId, @RequestBody UserUpdateDto dto) {
        return adminService.updateUser(userId, dto);
    }

    // Получение всех пользователей (только для админа? Оставим для MVP)
    @GetMapping
    public Page<UserResponseDto> getUsers(Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    @GetMapping("/find")
    public Page<User> getUser(@RequestParam("firstname") String firstName,
                              @RequestParam("lastname") String lastName,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
        return adminService.getUsersByName(firstName, lastName, page, size);
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

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
    }
}
