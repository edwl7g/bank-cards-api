package com.example.bankcards.controller.web;

import com.example.bankcards.dto.CardDetailsDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.UserService;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/web/user")
public class UserWebController {

    private final UserService userService;

    public UserWebController(UserService userService) {
        this.userService = userService;
    }

    // Список своих карт
    @GetMapping("/cards")
    public String getUserCards(@AuthenticationPrincipal CustomUserDetails currentUser,
                               @RequestParam(required = false) String search,
                               @PageableDefault(size = 10) Pageable pageable,
                               Model model) {
        Page<CardResponseDto> cards = userService.getUserCards(currentUser.getUserId(), search, pageable);
        model.addAttribute("cards", cards);
        model.addAttribute("search", search);
        return "user/cards";
    }

    // Запрос на блокировку карты
    @PostMapping("/cards/{cardId}/block-request")
    public String requestCardBlock(@PathVariable Long cardId,
                                   @AuthenticationPrincipal CustomUserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {
        userService.requestCardBlock(cardId, currentUser.getUserId());
        redirectAttributes.addFlashAttribute("message", "Запрос на блокировку отправлен");
        return "redirect:/web/user/cards";
    }

    // Перевод между своими картами (форма)
    @GetMapping("/transfer")
    public String showTransferForm(@AuthenticationPrincipal CustomUserDetails currentUser,
                                   Model model) {
        // Можно подгрузить список карт для выбора
        model.addAttribute("fromCardId", null);
        model.addAttribute("toCardId", null);
        model.addAttribute("amount", null);
        return "user/transfer";
    }

    @PostMapping("/transfer")
    public String transferBetweenOwnCards(@RequestParam Long fromCardId,
                                          @RequestParam Long toCardId,
                                          @RequestParam @Positive BigDecimal amount,
                                          @AuthenticationPrincipal CustomUserDetails currentUser,
                                          RedirectAttributes redirectAttributes) {
        userService.transferBetweenOwnCards(fromCardId, toCardId, amount, currentUser.getUserId());
        redirectAttributes.addFlashAttribute("message", "Перевод выполнен успешно");
        return "redirect:/web/user/cards";
    }

    // Баланс карты
    @GetMapping("/cards/{cardId}/balance")
    public String getCardBalance(@PathVariable Long cardId,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 Model model) {
        BigDecimal balance = userService.getCardBalance(cardId, currentUser.getUserId());
        model.addAttribute("balance", balance);
        model.addAttribute("cardId", cardId);
        return "user/balance";
    }

    // Детали карты (для пользователя)
    @GetMapping("/cards/{cardId}")
    public String getCardDetails(@PathVariable Long cardId,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 Model model) {
        CardDetailsDto details = userService.getCardByIdForUser(cardId, currentUser.getUserId());
        model.addAttribute("card", details);
        return "user/card-details";
    }

    // Поиск по номеру карты (для пользователя)
    @GetMapping("/cards/by-number")
    public String getCardByNumber(@RequestParam("number") String cardNumber,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  Model model) {
        CardDetailsDto details = userService.getCardByNumberForUser(cardNumber, currentUser.getUserId());
        model.addAttribute("card", details);
        return "user/card-details";
    }
}
