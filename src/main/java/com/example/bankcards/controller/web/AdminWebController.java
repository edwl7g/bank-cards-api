package com.example.bankcards.controller.web;

import com.example.bankcards.dto.*;
import com.example.bankcards.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/admin")
public class AdminWebController {

    private final AdminService adminService;

    public AdminWebController(AdminService adminService) {
        this.adminService = adminService;
    }

    // === Управление пользователями ===

    @GetMapping("/users")
    public String listUsers(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<UserResponseDto> users = adminService.getAllUsers(pageable);
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/find")
    public String findUsers(@RequestParam String firstName,
                            @RequestParam String lastName,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {
        Page<UserSummaryDto> users = adminService.getUsersByName(firstName, lastName, page, size);
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("userDto", new CreateUserDto(null, null, null, null, null, null, null, null));
        return "admin/create-user";
    }

    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute("userDto") CreateUserDto dto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Оставляем модель, чтобы ошибки отобразились
            return "admin/create-user";
        }
        try {
            adminService.createUser(dto);
            redirectAttributes.addFlashAttribute("message", "Пользователь создан");
            return "redirect:/web/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/create-user";
        }
    }

    @GetMapping("/users/{userId}/edit")
    public String showEditUserForm(@PathVariable Long userId, Model model) {
        // Получаем текущие данные пользователя (например, через AdminService метод)
        // Предположим, есть метод getUserForUpdate(userId) возвращающий UserUpdateDto
        UserUpdateDto dto = adminService.getUserForUpdate(userId); // добавим в AdminService
        model.addAttribute("userDto", dto);
        model.addAttribute("userId", userId);
        return "admin/edit-user";
    }

    @PostMapping("/users/{userId}/edit")
    public String updateUser(@PathVariable Long userId,
                             @Valid @ModelAttribute("userDto") UserUpdateDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/edit-user";
        }
        adminService.updateUser(userId, dto);
        redirectAttributes.addFlashAttribute("message", "Пользователь обновлён");
        return "redirect:/web/admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        adminService.deleteUser(userId);
        redirectAttributes.addFlashAttribute("message", "Пользователь удалён");
        return "redirect:/web/admin/users";
    }

    // === Управление счетами ===

    @GetMapping("/accounts/create")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("accountDto", new CreateBankAccountDto(null, null));
        return "admin/create-account";
    }

    @PostMapping("/accounts/create")
    public String createAccount(@Valid @ModelAttribute("accountDto") CreateBankAccountDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/create-account";
        }
        adminService.createAccount(dto);
        redirectAttributes.addFlashAttribute("message", "Счёт создан");
        return "redirect:/web/admin/users";
    }

    // === Управление картами ===

    @GetMapping("/cards")
    public String listCards(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<CardResponseDto> cards = adminService.getAllCards(pageable);
        model.addAttribute("cards", cards);
        return "admin/cards";
    }

    @GetMapping("/cards/create")
    public String showCreateCardForm(Model model) {
        model.addAttribute("cardDto", new CreateCardDto(null, null, null));
        return "admin/create-card";
    }

    @PostMapping("/cards/create")
    public String createCard(@Valid @ModelAttribute("cardDto") CreateCardDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/create-card";
        }
        adminService.createCard(dto);
        redirectAttributes.addFlashAttribute("message", "Карта создана");
        return "redirect:/web/admin/cards";
    }

    @PostMapping("/cards/{cardId}/block")
    public String blockCard(@PathVariable Long cardId, RedirectAttributes redirectAttributes) {
        adminService.blockCard(cardId);
        redirectAttributes.addFlashAttribute("message", "Карта заблокирована");
        return "redirect:/web/admin/cards";
    }

    @PostMapping("/cards/{cardId}/activate")
    public String activateCard(@PathVariable Long cardId, RedirectAttributes redirectAttributes) {
        adminService.activateCard(cardId);
        redirectAttributes.addFlashAttribute("message", "Карта активирована");
        return "redirect:/web/admin/cards";
    }

    @PostMapping("/cards/{cardId}/delete")
    public String deleteCard(@PathVariable Long cardId, RedirectAttributes redirectAttributes) {
        adminService.deleteCard(cardId);
        redirectAttributes.addFlashAttribute("message", "Карта удалена");
        return "redirect:/web/admin/cards";
    }

    @GetMapping("/cards/{cardId}")
    public String getCardDetails(@PathVariable Long cardId, Model model) {
        CardDetailsDto details = adminService.getCardByIdForAdmin(cardId);
        model.addAttribute("card", details);
        return "admin/card-details";
    }

    @GetMapping("/cards/by-number")
    public String getCardByNumber(@RequestParam("number") String cardNumber, Model model) {
        CardDetailsDto details = adminService.getCardByNumberForAdmin(cardNumber);
        model.addAttribute("card", details);
        return "admin/card-details";
    }
}
