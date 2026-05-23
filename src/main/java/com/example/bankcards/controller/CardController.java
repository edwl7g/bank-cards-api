package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/card")
public class CardController {

    private final AdminService adminService;

    public CardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public Page<CardResponseDto> getAllCards(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return adminService.getAllCards(pageable);
    }

    // Создание карты
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Card createCard(@RequestBody CreateCardDto dto) {
        return adminService.createCard(dto);
    }

    // Блокировка карты
    @PostMapping("/{cardId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockCard(@PathVariable Long cardId) {
        adminService.blockCard(cardId);
    }

    // Активация карты
    @PostMapping("/{cardId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateCard(@PathVariable Long cardId) {
        adminService.activateCard(cardId);
    }

    // Удаление карты
    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long cardId) {
        adminService.deleteCard(cardId);
    }

}
